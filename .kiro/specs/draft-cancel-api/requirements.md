# Requirements Document

## Introduction

DRAFT 상태의 대출 신청서를 취소(소프트 삭제)하는 API를 구현한다.
사용자가 대출 신청 과정에서 신청을 포기할 경우, 해당 신청서의 상태를 CANCELLED로 변경하여 논리적으로 삭제 처리한다.
금융 서비스 특성상 감사 추적을 위해 실제 DB row는 삭제하지 않는다.

## Glossary

- **System**: DRAFT 취소 API를 처리하는 sofit-user 백엔드 서버
- **LoanApplication**: 대출 신청 엔티티. 사용자가 대출 상품에 대해 생성한 신청 건
- **DRAFT**: 대출 신청서가 최초 생성된 후 아직 최종 제출되지 않은 상태
- **CANCELLED**: 사용자가 DRAFT 상태의 신청서를 취소한 후의 상태
- **ApplicationId**: 대출 신청 건의 고유 식별자 (Long 타입)
- **UserId**: 세션에서 획득한 현재 로그인 사용자의 고유 식별자

## Requirements

### Requirement 1: DRAFT 대출 신청서 취소

**User Story:** As a 소상공인 고객, I want to DRAFT 상태의 대출 신청서를 취소하고 싶다, so that 불필요한 신청서를 정리하고 새로운 신청을 시작할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 사용자가 DELETE /api/loan-applications/{applicationId} 요청을 전송하고 해당 applicationId의 LoanApplication이 존재하며 요청자 본인 소유이고 status가 DRAFT이면, THE System SHALL 해당 LoanApplication의 status를 CANCELLED로 변경한다.
2. WHEN 취소 요청이 성공적으로 처리되면, THE System SHALL isSuccess=true, code="LOAN2017", message="신청서가 취소되었습니다." 응답을 반환한다.
3. THE System SHALL 실제 데이터베이스 row를 삭제하지 않고 status 필드만 CANCELLED로 변경하는 소프트 삭제를 수행한다.
4. IF applicationId에 해당하는 LoanApplication이 존재하지 않으면, THEN THE System SHALL 존재하지 않는 대출 신청임을 나타내는 에러 응답을 반환한다.
5. IF 요청된 LoanApplication이 현재 인증된 사용자의 소유가 아니면, THEN THE System SHALL 본인의 신청만 처리할 수 있음을 나타내는 에러 응답을 반환한다.
6. IF 요청된 LoanApplication의 status가 DRAFT가 아니면, THEN THE System SHALL DRAFT 상태의 신청만 취소할 수 있음을 나타내는 에러 응답을 반환한다.

### Requirement 2: 신청서 존재 여부 검증

**User Story:** As a 시스템, I want to 존재하지 않는 applicationId에 대한 취소 요청을 거부하고 싶다, so that 잘못된 요청에 대해 명확한 오류를 반환할 수 있다.

#### Acceptance Criteria

1. WHEN 데이터베이스에 해당 applicationId를 가진 LoanApplication 레코드가 존재하지 않는 상태에서 취소 요청이 수신되면, THE System SHALL HTTP 404 응답과 함께 APPLICATION_NOT_FOUND 에러 코드를 반환한다.
2. IF applicationId가 양의 정수가 아닌 값(음수, 0, 숫자가 아닌 문자열)으로 전달되면, THEN THE System SHALL HTTP 400 응답과 함께 잘못된 요청임을 나타내는 에러를 반환한다.
3. WHEN 취소 요청에 대해 APPLICATION_NOT_FOUND 에러가 반환되면, THE System SHALL 기존 데이터에 어떠한 상태 변경도 수행하지 않는다.

### Requirement 3: 본인 소유 검증

**User Story:** As a 시스템, I want to 타인의 대출 신청서 취소를 차단하고 싶다, so that 사용자 데이터의 무단 접근을 방지할 수 있다.

#### Acceptance Criteria

1. WHEN 세션 UserId와 LoanApplication 소유자의 UserId가 일치하지 않는 취소 요청이 수신되면, THE System SHALL HTTP 403 응답과 함께 APPLICATION_NOT_OWNED 에러 코드를 반환한다.
2. IF 세션 UserId와 LoanApplication 소유자의 UserId가 일치하지 않으면, THEN THE System SHALL 해당 LoanApplication의 상태를 변경하지 않고 요청을 거부한다.
3. WHEN applicationId에 해당하는 LoanApplication이 존재하고 본인 소유가 아닌 경우, THE System SHALL 해당 LoanApplication의 존재 여부를 응답에 노출하지 않고 동일한 403 APPLICATION_NOT_OWNED 응답을 반환한다.

### Requirement 4: DRAFT 상태 검증

**User Story:** As a 시스템, I want to DRAFT 상태가 아닌 신청서의 취소를 차단하고 싶다, so that 심사 중이거나 완료된 신청서가 부적절하게 취소되는 것을 방지할 수 있다.

#### Acceptance Criteria

1. WHEN LoanApplication의 status가 DRAFT인 상태에서 취소 요청이 수신되면, THE System SHALL 해당 신청서의 status를 CANCELLED로 변경하고 성공 응답을 반환한다.
2. IF 취소 요청 대상 LoanApplication의 status가 DRAFT가 아닌 경우, THEN THE System SHALL HTTP 400 응답과 함께 APPLICATION_NOT_DRAFT 에러 코드를 반환하고, 해당 신청서의 status를 변경하지 않는다.

### Requirement 5: 기존 DRAFT 조회 로직 영향 없음

**User Story:** As a 시스템, I want to CANCELLED 상태가 된 신청서가 기존 DRAFT 조회 로직에서 자동으로 제외되길 원한다, so that 별도의 조회 로직 수정 없이 정상 동작할 수 있다.

#### Acceptance Criteria

1. WHEN LoanApplication의 status가 CANCELLED로 변경된 후 동일 사용자가 동일 상품에 대해 DRAFT 존재 여부를 확인하면, THE System SHALL 해당 CANCELLED 신청서를 DRAFT로 취급하지 않고 hasDraft=false, applicationId=null, resumeStep=null을 반환한다.
2. WHEN LoanApplication의 status가 CANCELLED로 변경된 후 동일 사용자가 동일 상품에 대해 새로운 신청을 시도하면, THE System SHALL 중복 신청 에러 없이 새 신청서를 DRAFT 상태로 생성한다.
3. IF 사용자가 CANCELLED 상태인 신청서의 applicationId로 이어가기(resume) 조회를 요청하면, THEN THE System SHALL 해당 신청서를 반환하지 않고 신청서를 찾을 수 없음을 나타내는 에러 응답을 반환한다.
