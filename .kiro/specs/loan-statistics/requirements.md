# Requirements Document

## Introduction

SoFit 은행원용 관리자 시스템(sofit-admin)에서 대출 신청 현황을 상태별로 집계하여 통계 데이터를 제공하는 API이다. 은행원이 대시보드에서 현재 대출 심사 파이프라인의 전체 현황을 한눈에 파악할 수 있도록 상태별 건수를 반환한다.

## Glossary

- **Statistics_API**: 대출 신청 현황 통계를 조회하는 REST API 엔드포인트 (`GET /api/admin/loan-applications/statistics`)
- **LoanApplication**: 대출 신청 엔티티. `loan_application` 테이블에 매핑되며 `status` 필드로 현재 심사 상태를 관리한다
- **ApplicationStatus**: 대출 신청의 상태를 나타내는 enum. DRAFT, SUBMITTED, CB_CHECKING, BASIC_REVIEW, S_CALCULATING, S_COMPLETED, SYSTEM_APPROVED, SYSTEM_REJECTED, MANAGER_REVIEW, APPROVED, REJECTED, CONTRACTED, EXECUTED, CANCELLED 값을 가진다
- **Pending**: SYSTEM_APPROVED 또는 SYSTEM_REJECTED 상태인 대출 신청 건의 합산 수량
- **ManagerReview**: MANAGER_REVIEW 상태인 대출 신청 건의 수량
- **Approved**: APPROVED 상태인 대출 신청 건의 수량
- **Rejected**: REJECTED 상태인 대출 신청 건의 수량
- **ApiResponse**: SoFit 공통 응답 포맷. isSuccess, code, message, result 필드로 구성된다
- **ADMIN_DEV**: 개발자 관리자 역할. 시스템 관리 및 통계 조회 권한을 가진다
- **ADMIN_BANK_TELLER**: 은행 창구 직원 역할. 대출 심사 처리 및 통계 조회 권한을 가진다
- **ADMIN_BANK_MANAGER**: 은행 지점장 역할. 대출 심사 최종 승인 및 통계 조회 권한을 가진다
- **허용 역할**: ADMIN_DEV, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER 중 하나의 역할을 가진 인증된 사용자만 Statistics_API에 접근 가능하다
- **GeneralErrorCode**: sofit-common 모듈에 정의된 공통 에러 코드 enum (COMMON4000, COMMON4001, COMMON4003, COMMON4004, COMMON5000)

## Requirements

### Requirement 1: 대출 현황 통계 조회

**User Story:** As a 관리자(ADMIN_DEV, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER), I want 대출 신청 건의 상태별 통계를 조회하고 싶다, so that 현재 심사 파이프라인의 전체 현황을 한눈에 파악할 수 있다.

#### Acceptance Criteria

1. WHEN 허용 역할(ADMIN_DEV, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER)을 가진 사용자가 `GET /api/admin/loan-applications/statistics` 요청을 보내면, THE Statistics_API SHALL LoanApplication 테이블에서 상태별 건수를 집계하여 Pending, ManagerReview, Approved, Rejected 각 항목을 0 이상의 정수 값으로 포함한 ApiResponse 형식으로 반환한다
2. WHEN 통계를 집계할 때, THE Statistics_API SHALL Pending 값을 SYSTEM_APPROVED 상태 건수와 SYSTEM_REJECTED 상태 건수의 합으로 산출한다
3. WHEN 통계를 집계할 때, THE Statistics_API SHALL ManagerReview 값을 MANAGER_REVIEW 상태 건수로 산출한다
4. WHEN 통계를 집계할 때, THE Statistics_API SHALL Approved 값을 APPROVED 상태 건수로 산출한다
5. WHEN 통계를 집계할 때, THE Statistics_API SHALL Rejected 값을 REJECTED 상태 건수로 산출한다
6. WHEN 통계 조회가 성공하면, THE Statistics_API SHALL 응답 코드 "COMMON2000"과 메시지 "성공입니다."를 반환한다
7. IF 해당 상태에 속하는 LoanApplication 건이 존재하지 않으면, THEN THE Statistics_API SHALL 해당 항목의 값을 0으로 반환한다

### Requirement 2: 인증 및 권한 검증

**User Story:** As a 시스템 관리자, I want 인증되지 않거나 권한이 없는 사용자의 통계 조회를 차단하고 싶다, so that 민감한 대출 현황 데이터가 보호된다.

#### Acceptance Criteria

1. IF 인증되지 않은 사용자가 Statistics_API에 접근하면, THEN THE Statistics_API SHALL HTTP 상태 코드 401과 함께 공통 응답 형식(isSuccess: false, code: "COMMON4001", message: "인증이 필요합니다.")을 반환한다
2. IF ADMIN_DEV, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER 역할이 아닌 인증된 사용자가 Statistics_API에 접근하면, THEN THE Statistics_API SHALL HTTP 상태 코드 403과 함께 공통 응답 형식(isSuccess: false, code: "COMMON4003", message: "권한이 없습니다.")을 반환한다
3. IF 사용자의 Redis 세션이 만료된 상태에서 Statistics_API에 접근하면, THEN THE Statistics_API SHALL 해당 세션을 무효화하고 HTTP 상태 코드 401과 함께 공통 응답 형식(isSuccess: false, code: "COMMON4001", message: "인증이 필요합니다.")을 반환한다

### Requirement 3: 서버 오류 처리

**User Story:** As a 관리자(ADMIN_DEV, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER), I want 서버 오류 발생 시 명확한 에러 응답을 받고 싶다, so that 문제 상황을 인지하고 관리자에게 문의할 수 있다.

#### Acceptance Criteria

1. IF Statistics_API 처리 중 예상치 못한 서버 오류가 발생하면, THEN THE Statistics_API SHALL HTTP 상태 코드 500과 함께 isSuccess가 false이고, 에러 코드 "COMMON5000", 메시지 "서버 에러, 관리자에게 문의 바랍니다."를 포함한 공통 응답 포맷(isSuccess, code, message)으로 반환한다
2. IF Statistics_API 처리 중 예상치 못한 서버 오류가 발생하면, THEN THE Statistics_API SHALL 오류 발생 이전에 커밋되지 않은 데이터 변경 사항을 롤백하여 기존 데이터 상태를 보존한다

### Requirement 4: 응답 형식 준수

**User Story:** As a 프론트엔드 개발자, I want 통계 API가 공통 응답 포맷을 준수하길 원한다, so that 기존 API 클라이언트 코드를 재사용할 수 있다.

#### Acceptance Criteria

1. WHEN 통계 API 요청이 성공하면, THE Statistics_API SHALL 응답 본문을 ApiResponse 형식(isSuccess: true, code: "COMMON2000", message: "성공입니다.", result: 통계 객체)으로 반환하고 HTTP 상태 코드 200을 응답한다
2. THE Statistics_API SHALL result 필드에 pending, managerReview, approved, rejected 4개의 필드를 포함하며, 각 필드의 값은 0 이상의 정수(int)로 반환한다
3. THE Statistics_API SHALL Content-Type 헤더를 "application/json"으로 설정하여 응답한다
4. IF 해당 상태의 대출 신청 건이 존재하지 않으면, THEN THE Statistics_API SHALL 해당 필드의 값을 0으로 반환한다 (null을 반환하지 않는다)
5. IF 통계 API 처리 중 서버 오류가 발생하면, THEN THE Statistics_API SHALL 응답 본문을 ApiResponse 형식(isSuccess: false, code: 에러 코드 문자열, message: 오류 원인을 나타내는 메시지 문자열, result: null)으로 반환한다
