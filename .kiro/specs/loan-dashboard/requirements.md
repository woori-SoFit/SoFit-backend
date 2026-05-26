# Requirements Document

## Introduction

은행원(BANK_ADMIN)이 대출 신청 현황을 조회할 수 있는 대시보드 API를 제공한다. 페이징, 심사 상태 필터, 담당자명 필터를 지원하며, 대출 신청 건별로 신청자명, 사업장명, 상품명, 담당 은행원명 등 관련 정보를 조인하여 반환한다.

## Glossary

- **Loan_Dashboard_API**: `/api/admin/loan-applications` 엔드포인트로 대출 신청 목록을 페이징 조회하는 REST API
- **LoanApplication**: 대출 신청 엔티티. 신청자(User), 상품(LoanProduct), 심사 상태(ApplicationStatus), 담당 은행원 ID 등을 포함
- **ApplicationStatus**: 대출 신청의 심사 단계를 나타내는 enum (DRAFT, SUBMITTED, CB_CHECKING, BASIC_REVIEW, SCB_CALCULATING, SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED, CONTRACTED, EXECUTED, CANCELLED)
- **Pageable**: Spring Data JPA의 페이징 처리 인터페이스. page(0부터 시작)와 size 파라미터로 제어
- **Converter**: Entity를 Response DTO로 변환하는 유틸리티 클래스

## Requirements

### Requirement 1: 대출 신청 목록 페이징 조회

**User Story:** As a 은행원(BANK_ADMIN), I want 대출 신청 목록을 페이징하여 조회하고 싶다, so that 대량의 신청 건을 효율적으로 관리할 수 있다.

#### Acceptance Criteria

1. WHEN page와 size 파라미터가 전달되면, THE Loan_Dashboard_API SHALL ApplicationStatus가 SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED 중 하나인 대출 신청 건만 조회하여 appliedAt 내림차순(최신순)으로 정렬한 해당 페이지의 결과를 반환한다
2. THE Loan_Dashboard_API SHALL 응답에 totalCount, totalPages, currentPage, size, applications 필드를 포함한다
3. IF 조회 결과가 없으면, THEN THE Loan_Dashboard_API SHALL applications를 빈 배열로, totalCount를 0으로, totalPages를 0으로 반환한다
4. THE Loan_Dashboard_API SHALL 각 application 항목에 applicationId, appliedAt, applicantName, businessName, productName, status, assignedBankerId, assigneeName 필드를 포함한다
5. IF size 파라미터가 전달되지 않으면, THEN THE Loan_Dashboard_API SHALL 기본값 10을 적용하여 조회한다
6. IF page 파라미터가 전달되지 않으면, THEN THE Loan_Dashboard_API SHALL 기본값 0(첫 페이지)을 적용하여 조회한다

### Requirement 2: 심사 상태 필터링

**User Story:** As a 은행원(BANK_ADMIN), I want 심사 상태별로 대출 신청 목록을 필터링하고 싶다, so that 특정 단계의 신청 건만 집중적으로 처리할 수 있다.

#### Acceptance Criteria

1. WHEN status 쿼리 파라미터에 유효한 값(SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED 중 하나)이 전달되면, THE Loan_Dashboard_API SHALL 해당 상태와 일치하는 대출 신청 건만 포함된 목록을 공통 응답 포맷으로 반환한다
2. WHEN status 쿼리 파라미터가 전달되지 않으면, THE Loan_Dashboard_API SHALL 5개 상태(SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED) 전체의 대출 신청 건을 포함한 목록을 반환한다
3. IF status 쿼리 파라미터 값이 허용된 5개 값(SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED) 중 하나가 아닌 경우, THEN THE Loan_Dashboard_API SHALL isSuccess=false, code=COMMON4000을 포함한 에러 응답을 반환한다
4. WHEN status 필터가 적용된 상태에서 해당 상태의 신청 건이 0건이면, THE Loan_Dashboard_API SHALL 빈 목록(result 내 빈 배열)을 포함한 정상 응답을 반환한다

### Requirement 3: 담당자명 필터링

**User Story:** As a 은행원(BANK_ADMIN), I want 담당자명으로 대출 신청 목록을 필터링하고 싶다, so that 특정 은행원이 담당하는 건만 조회할 수 있다.

#### Acceptance Criteria

1. WHEN assigneeName 파라미터가 1자 이상 50자 이하의 문자열로 전달되면, THE Loan_Dashboard_API SHALL 해당 담당자명과 정확히 일치하는(exact match) 대출 신청 건만 반환한다
2. WHEN assigneeName 파라미터가 전달되지 않거나 빈 문자열("")로 전달되면, THE Loan_Dashboard_API SHALL 담당자 미배정 건을 포함한 전체 대출 신청 건을 반환한다
3. WHEN assigneeName 파라미터와 일치하는 담당자가 존재하지 않으면, THE Loan_Dashboard_API SHALL 빈 목록을 반환한다

### Requirement 4: 관련 엔티티 정보 조인

**User Story:** As a 은행원(BANK_ADMIN), I want 대출 신청 목록에서 신청자명, 사업장명, 상품명, 담당 은행원명을 함께 확인하고 싶다, so that 별도 상세 조회 없이 목록에서 핵심 정보를 파악할 수 있다.

#### Acceptance Criteria

1. THE Loan_Dashboard_API SHALL LoanApplication의 user_id를 이용하여 Users 테이블에서 name을 조회하고 applicantName으로 반환한다
2. WHEN LoanApplication의 user_id에 해당하는 BusinessProfile이 1건 이상 존재하면, THE Loan_Dashboard_API SHALL 해당 사용자의 BusinessProfile 중 가장 최근에 생성된 1건의 business_name을 조회하고 businessName으로 반환한다
3. THE Loan_Dashboard_API SHALL LoanApplication의 product_id를 이용하여 LoanProduct 테이블에서 product_name을 조회하고 productName으로 반환한다
4. THE Loan_Dashboard_API SHALL LoanApplication의 assigned_banker_id를 이용하여 Users 테이블에서 name을 조회하고 assigneeName으로 반환한다

### Requirement 5: 공통 응답 포맷 준수

**User Story:** As a 프론트엔드 개발자, I want 대시보드 API가 프로젝트 공통 응답 포맷을 따르길 원한다, so that 기존 API와 동일한 방식으로 응답을 처리할 수 있다.

#### Acceptance Criteria

1. THE Loan_Dashboard_API SHALL 성공 시 isSuccess=true, code="COMMON2000", message="성공입니다." 형식으로 응답한다
2. IF 서버 내부 오류가 발생하면, THEN THE Loan_Dashboard_API SHALL 공통 에러 코드(COMMON5000)로 응답한다
3. IF 잘못된 요청 파라미터가 전달되면, THEN THE Loan_Dashboard_API SHALL 공통 에러 코드(COMMON4000)로 응답한다
