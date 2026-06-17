# Requirements Document

## Introduction

지점장 결재 조회 API는 지점장(ADMIN_BANK_MANAGER) 또는 개발자(ADMIN_DEV) 역할을 가진 관리자가 현재 지점장 리뷰 대기 중인 대출 신청 건 목록을 조회하는 기능이다. `GET /api/admin/manager/loan-applications` 엔드포인트를 통해 `MANAGER_REVIEW` 상태의 대출 신청 건을 리스트로 반환한다.

## Glossary

- **Manager_Approval_API**: 지점장 결재 조회 API 시스템. `GET /api/admin/manager/loan-applications` 엔드포인트를 제공한다.
- **LoanApplication**: 대출 신청 엔티티. applicationId, user, product, status, requestedAmount, assignedBankerId, appliedAt 등의 필드를 포함한다.
- **BusinessProfile**: 사업자 프로필 엔티티. user와 연관되며 businessName 필드를 포함한다.
- **User**: 사용자 엔티티. userId, name, role 필드를 포함한다.
- **UserRole**: 사용자 역할 열거형. USER, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV 값을 가진다.
- **ApplicationStatus**: 대출 신청 상태 열거형. MANAGER_REVIEW는 지점장 리뷰 대기 상태를 의미한다.
- **SecurityUtil**: SecurityContext에서 현재 인증된 사용자의 userId를 추출하는 유틸리티 클래스.
- **ApiResponse**: 공통 응답 포맷. isSuccess, code, message, result 필드를 포함한다.

## Requirements

### Requirement 1: 지점장 결재 대기 목록 조회

**User Story:** 지점장으로서, 현재 결재 대기 중인 대출 신청 건 목록을 조회하고 싶다. 그래야 심사 대상 건을 파악하고 결재 처리를 진행할 수 있다.

#### Acceptance Criteria

1. WHEN GET `/api/admin/manager/loan-applications` 요청을 수신하면, THE Manager_Approval_API SHALL LoanApplication 테이블에서 status가 MANAGER_REVIEW인 건만 조회하여 appliedAt 기준 오름차순(오래된 건 우선)으로 정렬된 리스트를 반환한다.
2. WHEN 조회 결과를 응답으로 구성할 때, THE Manager_Approval_API SHALL 각 대출 신청 건에 대해 다음 필드를 응답에 포함한다: id(applicationId), applicationDate(appliedAt을 yyyy-MM-dd 형식으로 변환), applicantName(신청자 이름), businessName(사업자 상호명), productName(대출 상품명), requestedByName(담당 은행원 이름), requestedAmount(신청 금액, 원 단위 정수).
3. WHEN 조회 결과가 0건이면, THE Manager_Approval_API SHALL 빈 배열을 result 필드에 담아 성공 응답을 반환한다.
4. THE Manager_Approval_API SHALL 성공 응답 시 isSuccess=true, code="COMMON2000", message="성공입니다." 형식의 ApiResponse를 반환한다.
5. IF 데이터베이스 조회 중 예외가 발생한 경우, THEN THE Manager_Approval_API SHALL code="COMMON5000", message="서버 에러, 관리자에게 문의 바랍니다." 형식의 에러 응답을 반환한다.

### Requirement 2: 접근 권한 검증

**User Story:** 시스템 관리자로서, 지점장 결재 조회 API에 권한이 없는 사용자가 접근하지 못하도록 하고 싶다. 그래야 민감한 대출 심사 정보가 보호된다.

#### Acceptance Criteria

1. IF 인증된 사용자의 역할이 ADMIN_BANK_MANAGER 또는 ADMIN_DEV이면, THEN THE Manager_Approval_API SHALL 요청을 정상 처리한다.
2. IF 인증된 사용자의 역할이 ADMIN_BANK_MANAGER 또는 ADMIN_DEV가 아니면, THEN THE Manager_Approval_API SHALL HTTP 403 상태와 함께 isSuccess=false, code="COMMON4003", message="권한이 없습니다." 에러 응답을 반환한다.
3. IF 요청에 유효한 세션이 존재하지 않으면(세션 만료 또는 미인증), THEN THE Manager_Approval_API SHALL HTTP 401 상태와 함께 isSuccess=false, code="COMMON4001", message="인증이 필요합니다." 에러 응답을 반환한다.
4. IF 요청에 유효한 세션이 존재하지 않고 역할 조건도 충족하지 않는 경우, THEN THE Manager_Approval_API SHALL 인증 검증을 권한 검증보다 우선 수행하여 HTTP 401 응답을 반환한다.

### Requirement 3: 응답 데이터 매핑

**User Story:** 프론트엔드 개발자로서, 결재 목록 화면에 필요한 데이터를 일관된 형식으로 받고 싶다. 그래야 화면을 정확하게 렌더링할 수 있다.

#### Acceptance Criteria

1. THE Manager_Approval_API SHALL id 필드를 LoanApplication.applicationId 값(Long 타입)으로 매핑한다.
2. WHEN LoanApplication.appliedAt 값이 존재하면, THE Manager_Approval_API SHALL applicationDate 필드를 해당 값에서 날짜 부분만 "yyyy-MM-dd" 형식의 문자열로 변환하여 매핑한다.
3. THE Manager_Approval_API SHALL applicantName 필드를 LoanApplication에 연관된 User 엔티티의 name 값으로 매핑한다.
4. THE Manager_Approval_API SHALL businessName 필드를 신청자(User)에 연관된 BusinessProfile 엔티티의 businessName 값으로 매핑한다.
5. THE Manager_Approval_API SHALL productName 필드를 LoanApplication에 연관된 LoanProduct 엔티티의 productName 값으로 매핑한다.
6. THE Manager_Approval_API SHALL requestedByName 필드를 LoanApplication.assignedBankerId에 해당하는 User 엔티티의 name 값으로 매핑한다.
7. THE Manager_Approval_API SHALL requestedAmount 필드를 LoanApplication.requestedAmount 값(Long 타입, 원 단위)으로 매핑한다.
8. IF LoanApplication.assignedBankerId가 null이면, THEN THE Manager_Approval_API SHALL requestedByName 필드를 null로 반환한다.
9. IF 신청자에 연관된 BusinessProfile이 존재하지 않으면, THEN THE Manager_Approval_API SHALL businessName 필드를 null로 반환한다.
10. IF LoanApplication.appliedAt 값이 null이면, THEN THE Manager_Approval_API SHALL applicationDate 필드를 null로 반환한다.
11. IF LoanApplication.requestedAmount 값이 null이면, THEN THE Manager_Approval_API SHALL requestedAmount 필드를 null로 반환한다.

### Requirement 4: 사용자 역할 조회 유틸리티

**User Story:** 백엔드 개발자로서, 현재 로그인한 사용자의 역할을 조회하는 재사용 가능한 방법을 사용하고 싶다. 그래야 권한 검증 로직을 일관되게 구현할 수 있다.

#### Acceptance Criteria

1. WHEN 역할 조회가 요청되면, THE Manager_Approval_API SHALL SecurityContext에서 현재 인증된 사용자의 userId를 추출하고, 해당 userId로 UserRepository를 조회하여 User 엔티티의 role 필드(USER, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV 중 하나)를 반환한다.
2. IF SecurityContext에 인증 정보가 존재하지 않거나 세션이 만료된 경우, THEN THE Manager_Approval_API SHALL 인증 실패 예외를 발생시키고 역할 조회를 수행하지 않는다.
3. IF 추출한 userId에 해당하는 사용자가 UserRepository에 존재하지 않거나 사용자 상태가 INACTIVE인 경우, THEN THE Manager_Approval_API SHALL 사용자 미존재 예외를 발생시킨다.
4. THE Manager_Approval_API SHALL 역할 조회 로직을 SecurityUtil과 UserRepository 조합의 재사용 가능한 단일 메서드로 제공하여, 권한 검증이 필요한 모든 서비스에서 동일한 방식으로 호출할 수 있도록 한다.

### Requirement 5: 에러 처리

**User Story:** 프론트엔드 개발자로서, API 호출 실패 시 명확한 에러 코드와 메시지를 받고 싶다. 그래야 사용자에게 적절한 안내를 표시할 수 있다.

#### Acceptance Criteria

1. IF 처리되지 않은 예외가 발생하면, THEN THE Manager_Approval_API SHALL HTTP 500 상태와 함께 code="COMMON5000" 에러 응답을 반환한다.
2. IF 요청 본문의 필수 필드 누락, 필드 타입 불일치, 또는 유효성 검증 실패가 발생하면, THEN THE Manager_Approval_API SHALL HTTP 400 상태와 함께 code="COMMON4000" 에러 응답을 반환한다.
3. THE Manager_Approval_API SHALL 모든 에러 응답을 isSuccess=false, code, message 필드를 포함하고 result 필드를 포함하지 않는 ApiResponse 형식으로 반환한다.
4. IF 유효한 세션 없이 요청이 수신되면, THEN THE Manager_Approval_API SHALL HTTP 401 상태와 함께 code="COMMON4001" 에러 응답을 반환한다.
5. IF 인증된 사용자가 권한이 없는 리소스에 접근하면, THEN THE Manager_Approval_API SHALL HTTP 403 상태와 함께 code="COMMON4003" 에러 응답을 반환한다.
6. IF 요청한 리소스가 존재하지 않으면, THEN THE Manager_Approval_API SHALL HTTP 404 상태와 함께 code="COMMON4004" 에러 응답을 반환한다.
