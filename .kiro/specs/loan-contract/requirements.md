# Requirements Document

## Introduction

대출 심사 승인(APPROVED) 후 사용자가 약정 체결을 요청하는 API입니다. 사용자는 승인된 한도 내에서 희망 금액을 지정하여 약정을 체결하며, 성공 시 대출 신청 상태가 CONTRACTED로 전이됩니다. 이 기능은 대출 신청 → 심사 → 승인 → 약정 → 실행 플로우에서 약정 단계를 담당합니다.

## Glossary

- **LoanApplication**: 대출 신청 엔티티. 사용자의 대출 신청 정보와 상태를 관리한다.
- **LoanDecision**: 심사 결정 엔티티. 승인/거절 결과와 승인 금액, 금리, 기간 정보를 포함한다.
- **Contract_Service**: 약정 체결 비즈니스 로직을 처리하는 서비스 컴포넌트.
- **ApplicationStatus**: 대출 신청의 상태를 나타내는 열거형 (DRAFT, SUBMITTED, APPROVED, CONTRACTED 등).
- **requestedAmount**: 사용자가 약정 시 요청하는 대출 금액.
- **approvedAmount**: 심사에서 승인된 최대 대출 가능 금액.

## Requirements

### Requirement 1: 약정 체결 엔드포인트

**User Story:** As a 대출 신청자, I want to 승인된 대출에 대해 약정 체결을 요청할 수 있다, so that 대출 실행 단계로 진행할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 POST /api/loan-applications/{applicationId}/contract 요청을 보내면, THE Contract_Service SHALL SecurityUtil.getCurrentUserId()로 현재 사용자를 식별한다
2. WHEN 약정 체결이 성공하면, THE Contract_Service SHALL applicationId와 "CONTRACTED" 상태를 포함한 응답을 반환한다
3. THE Contract_Service SHALL 요청 본문의 requestedAmount 필드에 대해 @NotNull 유효성 검증을 수행한다

### Requirement 2: 상태 전이 처리

**User Story:** As a 시스템, I want to 약정 체결 시 대출 신청 상태를 CONTRACTED로 변경한다, so that 대출 진행 상태를 정확히 추적할 수 있다.

#### Acceptance Criteria

1. WHEN 약정 체결이 성공하면, THE LoanApplication SHALL 상태를 CONTRACTED로 변경한다
2. WHEN 약정 체결이 성공하면, THE LoanApplication SHALL requestedAmount를 사용자가 요청한 금액으로 설정한다

### Requirement 3: 금액 검증

**User Story:** As a 시스템, I want to 요청 금액이 승인 금액을 초과하지 않도록 검증한다, so that 승인 범위 내에서만 약정이 체결된다.

#### Acceptance Criteria

1. WHEN requestedAmount가 approvedAmount 이하이면, THE Contract_Service SHALL 약정 체결을 정상 처리한다
2. IF requestedAmount가 approvedAmount를 초과하면, THEN THE Contract_Service SHALL AMOUNT_EXCEEDS_APPROVED 예외를 발생시킨다

### Requirement 4: 상태 유효성 검증

**User Story:** As a 시스템, I want to APPROVED 상태인 신청에 대해서만 약정 체결을 허용한다, so that 비정상적인 상태 전이를 방지한다.

#### Acceptance Criteria

1. WHILE 대출 신청 상태가 APPROVED인 경우, THE Contract_Service SHALL 약정 체결 요청을 허용한다
2. IF 대출 신청 상태가 APPROVED가 아니면, THEN THE Contract_Service SHALL INVALID_STATUS 예외를 발생시킨다

### Requirement 5: 신청 건 조회 및 소유권 검증

**User Story:** As a 시스템, I want to 대출 신청 건의 존재 여부와 소유권을 검증한다, so that 다른 사용자의 신청에 접근할 수 없다.

#### Acceptance Criteria

1. WHEN applicationId와 userId로 대출 신청을 조회할 때, THE Contract_Service SHALL 해당 조합으로 LoanApplication을 조회한다
2. IF applicationId와 userId 조합에 해당하는 대출 신청이 존재하지 않으면, THEN THE Contract_Service SHALL APPLICATION_NOT_FOUND 예외를 발생시킨다

### Requirement 6: 심사 결정 조회

**User Story:** As a 시스템, I want to 심사 결정 정보를 조회하여 승인 금액을 확인한다, so that 금액 검증의 기준값을 확보한다.

#### Acceptance Criteria

1. WHEN 약정 체결 처리 시, THE Contract_Service SHALL applicationId로 LoanDecision을 조회한다
2. IF 해당 applicationId에 대한 LoanDecision이 존재하지 않으면, THEN THE Contract_Service SHALL LOAN_DECISION_NOT_FOUND 예외를 발생시킨다
