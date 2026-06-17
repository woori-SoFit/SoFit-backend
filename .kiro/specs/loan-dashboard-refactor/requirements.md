# Requirements Document

## Introduction

은행원(BANK_ADMIN)이 대출 신청 현황을 조회할 수 있는 대시보드 API를 수정한다. 기존 `assigneeName` 필터와 `assignedBankerId` 쿼리 파라미터를 삭제하고, Redis 세션 기반의 `myOnly` 옵션으로 대체한다. 응답 필드명을 `applications`에서 `contents`로 변경한다.

## Glossary

- **Loan_Dashboard_API**: `/api/admin/loan-applications` 엔드포인트로 대출 신청 목록을 페이징 조회하는 REST API
- **LoanApplication**: 대출 신청 엔티티. 신청자(User), 상품(LoanProduct), 심사 상태(ApplicationStatus), 담당 은행원 ID 등을 포함
- **ApplicationStatus**: 대출 신청의 심사 단계를 나타내는 enum (DRAFT, SUBMITTED, CB_CHECKING, BASIC_REVIEW, SCB_CALCULATING, SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED, CONTRACTED, EXECUTED, CANCELLED)
- **Dashboard_Statuses**: 대시보드 조회 대상 상태 목록 (SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED)
- **Pageable**: Spring Data JPA의 페이징 처리 인터페이스. page(0부터 시작)와 size 파라미터로 제어
- **Redis_Session**: Redis에 저장된 사용자 세션 정보. user_id를 포함하며 인증된 은행원을 식별하는 데 사용
- **myOnly**: 본인 담당 건만 필터링하는 Boolean 쿼리 파라미터. true이면 세션의 user_id와 assignedBankerId가 일치하는 건만 반환
- **Converter**: Entity를 Response DTO로 변환하는 유틸리티 클래스

## Requirements

### Requirement 1: 대출 신청 목록 페이징 조회

**User Story:** As a 은행원(BANK_ADMIN), I want 대출 신청 목록을 페이징하여 조회하고 싶다, so that 대량의 신청 건을 효율적으로 관리할 수 있다.

#### Acceptance Criteria

1. WHEN page(0-based)와 size 파라미터가 전달되면, THE Loan_Dashboard_API SHALL ApplicationStatus가 SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED 중 하나인 대출 신청 건만 조회하여 appliedAt 내림차순(최신순)으로 정렬한 해당 페이지의 결과를 반환한다
2. THE Loan_Dashboard_API SHALL 응답에 totalCount, totalPages, currentPage, size, contents 필드를 포함한다
3. IF 조회 결과가 없으면, THEN THE Loan_Dashboard_API SHALL contents를 빈 배열로, totalCount를 0으로, totalPages를 0으로 반환한다
4. THE Loan_Dashboard_API SHALL 각 contents 항목에 applicationId, appliedAt, applicantName, businessName, productName, status, assignedBankerId, assigneeName 필드를 포함한다
5. IF size 파라미터가 전달되지 않으면, THEN THE Loan_Dashboard_API SHALL 기본값 10을 적용하여 조회한다
6. IF page 파라미터가 전달되지 않으면, THEN THE Loan_Dashboard_API SHALL 기본값 0(첫 페이지)을 적용하여 조회한다
7. IF page 파라미터가 음수이면, THEN THE Loan_Dashboard_API SHALL isSuccess=false, code=COMMON4000을 포함한 에러 응답을 반환한다
8. IF size 파라미터가 1 미만이거나 100 초과이면, THEN THE Loan_Dashboard_API SHALL isSuccess=false, code=COMMON4000을 포함한 에러 응답을 반환한다

### Requirement 2: 심사 상태 필터링

**User Story:** As a 은행원(BANK_ADMIN), I want 심사 상태별로 대출 신청 목록을 필터링하고 싶다, so that 특정 단계의 신청 건만 집중적으로 처리할 수 있다.

#### Acceptance Criteria

1. WHEN status 쿼리 파라미터에 유효한 값(SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED 중 하나)이 전달되면, THE Loan_Dashboard_API SHALL 해당 상태와 일치하는 대출 신청 건만 포함된 목록을 isSuccess=true 및 페이징 메타데이터(totalCount, totalPages, currentPage, size)와 함께 appliedAt 내림차순으로 정렬하여 반환한다
2. WHEN status 쿼리 파라미터가 전달되지 않으면, THE Loan_Dashboard_API SHALL 5개 상태(SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED) 전체의 대출 신청 건을 appliedAt 내림차순으로 정렬하여 페이징 메타데이터와 함께 반환한다
3. IF status 쿼리 파라미터 값이 허용된 5개 값(SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED) 중 하나가 아닌 경우, THEN THE Loan_Dashboard_API SHALL HTTP 400 상태코드와 함께 isSuccess=false, code=COMMON4000을 포함한 에러 응답을 반환한다
4. WHEN status 필터가 적용된 상태에서 해당 상태의 신청 건이 0건이면, THE Loan_Dashboard_API SHALL totalCount=0, contents 빈 배열을 포함한 isSuccess=true 정상 응답을 반환한다

### Requirement 3: myOnly 필터링 (본인 담당 건 조회)

**User Story:** As a 은행원(BANK_ADMIN), I want 본인이 담당하는 대출 신청 건만 필터링하여 조회하고 싶다, so that 내가 처리해야 할 건을 빠르게 파악할 수 있다.

#### Acceptance Criteria

1. WHEN myOnly 파라미터가 true로 전달되면, THE Loan_Dashboard_API SHALL Redis_Session에서 현재 로그인한 은행원의 user_id를 조회하고, Dashboard_Statuses(SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED)에 해당하는 건 중 assignedBankerId가 해당 user_id와 일치하는 대출 신청 건만 반환한다
2. WHEN myOnly 파라미터가 true이고 status 필터가 함께 전달되면, THE Loan_Dashboard_API SHALL myOnly 조건(본인 담당)과 status 필터 조건을 AND로 결합하여 적용한다
3. WHEN myOnly 파라미터가 false로 전달되면, THE Loan_Dashboard_API SHALL 담당자와 무관하게 Dashboard_Statuses에 해당하는 모든 대출 신청 건을 반환한다
4. WHEN myOnly 파라미터가 전달되지 않으면, THE Loan_Dashboard_API SHALL 기본값 false를 적용하여 전체 건을 반환한다
5. WHEN myOnly 파라미터가 true이고 본인 담당 건이 0건이면, THE Loan_Dashboard_API SHALL contents를 빈 배열로, totalCount를 0으로 반환한다
6. WHEN myOnly 파라미터가 true이면, THE Loan_Dashboard_API SHALL 페이징을 유지하여 page와 size에 따른 결과를 반환한다
7. IF myOnly 파라미터에 Boolean으로 파싱할 수 없는 값이 전달되면, THEN THE Loan_Dashboard_API SHALL isSuccess=false, code=COMMON4000을 포함한 에러 응답을 반환한다

### Requirement 4: 관련 엔티티 정보 조인

**User Story:** As a 은행원(BANK_ADMIN), I want 대출 신청 목록에서 신청자명, 사업장명, 상품명, 담당 은행원명을 함께 확인하고 싶다, so that 별도 상세 조회 없이 목록에서 핵심 정보를 파악할 수 있다.

#### Acceptance Criteria

1. THE Loan_Dashboard_API SHALL LoanApplication의 user_id를 이용하여 Users 테이블에서 name을 조회하고 applicantName으로 반환한다
2. WHEN LoanApplication의 user_id에 해당하는 BusinessProfile이 1건 이상 존재하면, THE Loan_Dashboard_API SHALL 해당 사용자의 BusinessProfile 중 createdAt이 가장 최신인 1건의 business_name을 조회하고 businessName으로 반환한다
3. IF LoanApplication의 user_id에 해당하는 BusinessProfile이 존재하지 않으면, THEN THE Loan_Dashboard_API SHALL businessName을 null로 반환한다
4. THE Loan_Dashboard_API SHALL LoanApplication의 product_id를 이용하여 LoanProduct 테이블에서 product_name을 조회하고 productName으로 반환한다
5. WHEN LoanApplication의 assigned_banker_id가 존재하면, THE Loan_Dashboard_API SHALL 해당 ID를 이용하여 Users 테이블에서 name을 조회하고 assigneeName으로 반환한다
6. IF LoanApplication의 assigned_banker_id가 null이면, THEN THE Loan_Dashboard_API SHALL assigneeName을 null로 반환한다
7. THE Loan_Dashboard_API SHALL 관련 엔티티(User, LoanProduct, 담당 은행원) 조회 시 단일 쿼리 또는 일괄 조회 방식을 사용하여 목록 건수만큼 추가 쿼리가 발생하지 않도록 한다

### Requirement 5: 공통 응답 포맷 준수

**User Story:** As a 프론트엔드 개발자, I want 대시보드 API가 프로젝트 공통 응답 포맷을 따르길 원한다, so that 기존 API와 동일한 방식으로 응답을 처리할 수 있다.

#### Acceptance Criteria

1. WHEN 대시보드 API 요청이 성공하면, THE Loan_Dashboard_API SHALL isSuccess=true, 도메인 성공 코드, 성공 메시지, result 객체를 포함하는 JSON 응답을 HTTP 200으로 반환한다
2. IF 서버 내부 오류가 발생하면, THEN THE Loan_Dashboard_API SHALL isSuccess=false, code="COMMON5000", message="서버 에러, 관리자에게 문의 바랍니다."를 포함하는 JSON 응답을 HTTP 500으로 반환한다
3. IF 잘못된 요청 파라미터가 전달되면, THEN THE Loan_Dashboard_API SHALL isSuccess=false, code="COMMON4000", message="잘못된 요청입니다."를 포함하는 JSON 응답을 HTTP 400으로 반환한다
4. THE Loan_Dashboard_API SHALL 모든 응답을 { isSuccess: boolean, code: string, message: string, result: object | null } 4개 필드로 구성된 공통 ApiResponse 포맷으로 반환한다
5. IF 도메인 예외(BaseException)가 발생하면, THEN THE Loan_Dashboard_API SHALL isSuccess=false와 해당 도메인 에러 코드를 포함하는 JSON 응답을 에러 코드에 대응하는 HTTP 상태 코드로 반환한다

### Requirement 6: 쿼리 파라미터 변경 사항

**User Story:** As a 프론트엔드 개발자, I want 기존 assignedBankerId와 assigneeName 쿼리 파라미터가 제거되고 myOnly로 대체되길 원한다, so that 간결한 API 인터페이스로 본인 담당 건 필터링을 수행할 수 있다.

#### Acceptance Criteria

1. IF assignedBankerId 쿼리 파라미터가 요청에 포함되면, THEN THE Loan_Dashboard_API SHALL 해당 파라미터를 무시하고 나머지 유효한 파라미터(page, size, status, myOnly)만으로 조회를 수행한다
2. IF assigneeName 쿼리 파라미터가 요청에 포함되면, THEN THE Loan_Dashboard_API SHALL 해당 파라미터를 무시하고 나머지 유효한 파라미터(page, size, status, myOnly)만으로 조회를 수행한다
3. THE Loan_Dashboard_API SHALL 요청 파라미터로 page, size, status, myOnly만 선언하며, Controller의 @RequestParam에 assignedBankerId와 assigneeName을 포함하지 않는다
4. THE Loan_Dashboard_API SHALL myOnly 파라미터를 Boolean 타입으로 선언하며 기본값은 false로 적용한다
