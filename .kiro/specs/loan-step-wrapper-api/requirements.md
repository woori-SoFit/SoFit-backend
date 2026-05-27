# Requirements Document

## Introduction

대출 신청 플로우에서 Step 1(신청 생성)과 Step 7(최종 제출) 사이의 중간 단계(Step 2~6)를 처리하는 래퍼 API를 구현한다. 각 래퍼 API는 범용 서비스를 내부적으로 호출하고, 해당 단계 완료 시 `lastCompletedStep`을 업데이트하는 역할을 담당한다. 범용 서비스는 대출 신청 컨텍스트를 알 필요 없이 독립적으로 동작하며, 래퍼 API가 대출 플로우의 단계 관리를 책임진다.

## Glossary

- **Wrapper_API**: 범용 서비스를 호출하고 lastCompletedStep을 업데이트하는 대출 신청 단계별 API
- **LoanApplication**: 대출 신청 엔티티. applicationId로 식별되며 status, lastCompletedStep 등의 상태를 가짐
- **LastCompletedStep**: 대출 신청 플로우에서 마지막으로 완료된 단계를 나타내는 enum (CONSENT_DONE, AUTH_DONE, BIZ_INFO_DONE, DATA_COLLECTED, MYBIZ_CONNECTED)
- **ConsentService**: 약관 동의를 처리하는 범용 서비스. 대출 약관과 마이데이터 약관 모두 처리 가능
- **AuthService**: 금융인증서 PIN 기반 본인인증을 처리하는 범용 서비스
- **BizInfoService**: 사업자등록번호 기반으로 사업자 정보를 조회하는 범용 서비스
- **MybizService**: 사업자등록번호 기반으로 마이비즈데이터를 수집하는 범용 서비스
- **LoanStepService**: 래퍼 API의 비즈니스 로직을 담당하는 서비스. 범용 서비스 호출 + lastCompletedStep 업데이트를 한 트랜잭션으로 처리
- **ApplicationId**: 대출 신청 건의 고유 식별자 (Long 타입)
- **BizNo**: 사업자등록번호. 하이픈 없는 10자리 숫자 문자열

## Requirements

### Requirement 1: 대출 약관 동의 (Step 2)

**User Story:** As a 소상공인 고객, I want to 대출 약관에 동의하여 대출 신청 절차를 진행하고 싶다, so that 다음 단계인 본인인증으로 넘어갈 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 applicationId와 1개 이상의 약관 동의 항목이 포함된 요청이 수신되면, THE Wrapper_API SHALL ConsentService를 호출하여 대출 약관 동의를 처리한다
2. WHEN ConsentService 호출이 성공하면, THE LoanStepService SHALL 해당 LoanApplication의 lastCompletedStep을 CONSENT_DONE으로 업데이트한다
3. WHEN 약관 동의 단계가 완료되면, THE Wrapper_API SHALL applicationId와 completedStep(CONSENT_DONE)을 포함한 성공 응답을 반환한다
4. IF 해당 applicationId의 LoanApplication이 존재하지 않으면, THEN THE Wrapper_API SHALL 404 에러를 반환한다
5. IF 해당 LoanApplication의 status가 DRAFT가 아니면, THEN THE Wrapper_API SHALL 400 에러를 반환한다
6. IF 약관 동의 목록에 필수 약관이 모두 포함되지 않으면, THEN THE Wrapper_API SHALL 필수 약관 미동의를 나타내는 400 에러를 반환하고 lastCompletedStep을 변경하지 않는다
7. IF 해당 LoanApplication의 lastCompletedStep이 null이 아니면(이미 Step 2 이상 완료된 상태), THEN THE Wrapper_API SHALL 단계 순서 위반을 나타내는 400 에러를 반환한다
8. IF ConsentService 호출이 실패하면, THEN THE Wrapper_API SHALL 서비스 처리 실패를 나타내는 에러를 반환하고 lastCompletedStep을 변경하지 않는다

### Requirement 2: 본인인증 - 금융인증서 PIN (Step 3)

**User Story:** As a 소상공인 고객, I want to 금융인증서 PIN으로 본인인증을 완료하고 싶다, so that 대출 신청의 신원 확인 절차를 통과할 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 applicationId와 6자리 숫자 형식의 PIN이 포함된 요청이 수신되면, THE Wrapper_API SHALL AuthService를 호출하여 본인인증을 처리한다
2. WHEN AuthService 호출이 성공하면, THE LoanStepService SHALL 해당 LoanApplication의 lastCompletedStep을 AUTH_DONE으로 업데이트한다
3. WHEN 본인인증 단계가 완료되면, THE Wrapper_API SHALL 성공 응답(applicationId, completedStep)을 반환한다
4. IF 해당 applicationId의 LoanApplication이 존재하지 않으면, THEN THE Wrapper_API SHALL 404 에러를 반환한다
5. IF 해당 LoanApplication의 status가 DRAFT가 아니면, THEN THE Wrapper_API SHALL 400 에러를 반환한다
6. IF PIN이 6자리 숫자 형식이 아니면, THEN THE Wrapper_API SHALL 400 에러를 반환한다
7. IF AuthService의 PIN 인증이 실패하면(PIN 불일치), THEN THE Wrapper_API SHALL 401 에러를 반환하고 lastCompletedStep을 변경하지 않는다
8. IF 해당 LoanApplication의 lastCompletedStep이 CONSENT_DONE이 아니면, THEN THE Wrapper_API SHALL 400 에러를 반환한다

### Requirement 3: 사업자 정보 확인 (Step 4)

**User Story:** As a 소상공인 고객, I want to 사업자 정보를 확인하여 대출 신청에 필요한 사업자 데이터를 검증하고 싶다, so that 정확한 사업자 정보 기반으로 대출 심사를 받을 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 applicationId와 bizNo가 포함된 요청이 수신되면, THE Wrapper_API SHALL BizInfoService를 호출하여 사업자 정보(상호명, 대표자명, 업종, 개업일)를 조회한다
2. WHEN BizInfoService 호출이 성공하면, THE LoanStepService SHALL 해당 LoanApplication의 lastCompletedStep을 BIZ_INFO_DONE으로 업데이트한다
3. WHEN 사업자 정보 확인 단계가 완료되면, THE Wrapper_API SHALL 성공 응답(applicationId, completedStep, 사업자 정보(상호명, 대표자명, 업종, 개업일))을 반환한다
4. IF 해당 applicationId의 LoanApplication이 존재하지 않으면, THEN THE Wrapper_API SHALL 404 에러를 반환한다
5. IF 해당 LoanApplication의 status가 DRAFT가 아니면, THEN THE Wrapper_API SHALL 400 에러를 반환한다
6. IF bizNo가 유효하지 않은 형식(10자리 숫자가 아닌 경우)이면, THEN THE Wrapper_API SHALL 400 에러를 반환한다
7. IF 해당 LoanApplication의 lastCompletedStep이 AUTH_DONE이 아니면, THEN THE Wrapper_API SHALL 400 에러를 반환하고 단계 순서가 올바르지 않음을 나타내는 에러 메시지를 포함한다
8. IF BizInfoService 호출이 실패하면(해당 bizNo로 사업자 정보를 찾을 수 없는 경우), THEN THE Wrapper_API SHALL 404 에러를 반환하고 lastCompletedStep을 변경하지 않는다

### Requirement 4: 마이데이터 수집 (Step 5)

**User Story:** As a 소상공인 고객, I want to 마이데이터 약관에 동의하고 마이데이터를 수집하고 싶다, so that 대출 심사에 필요한 금융 데이터를 제공할 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 applicationId와 1개 이상의 마이데이터 약관 동의 항목이 포함된 요청이 수신되면, THE Wrapper_API SHALL ConsentService를 호출하여 마이데이터 약관 동의를 처리한다
2. WHEN 마이데이터 약관 동의가 완료되면, THE LoanStepService SHALL 해당 LoanApplication에 연결된 사용자의 bizNo를 기반으로 마이데이터 수집을 수행한다
3. WHEN 마이데이터 수집이 완료되면, THE LoanStepService SHALL 해당 LoanApplication의 lastCompletedStep을 DATA_COLLECTED로 업데이트한다
4. WHEN 마이데이터 수집 단계가 완료되면, THE Wrapper_API SHALL 성공 응답(applicationId, completedStep)을 반환한다
5. IF 해당 applicationId의 LoanApplication이 존재하지 않으면, THEN THE Wrapper_API SHALL 404 에러를 반환한다
6. IF 해당 LoanApplication의 status가 DRAFT가 아니면, THEN THE Wrapper_API SHALL 400 에러를 반환한다
7. IF 해당 LoanApplication의 lastCompletedStep이 BIZ_INFO_DONE이 아니면, THEN THE Wrapper_API SHALL 이전 단계 미완료를 나타내는 400 에러를 반환한다
8. IF 마이데이터 약관 동의 목록이 비어있으면, THEN THE Wrapper_API SHALL 필수 항목 누락을 나타내는 400 에러를 반환한다
9. IF 마이데이터 수집 처리 중 외부 서비스 호출이 실패하면, THEN THE Wrapper_API SHALL 500 에러를 반환하고 lastCompletedStep을 변경하지 않는다

### Requirement 5: 마이비즈데이터 연동 (Step 6)

**User Story:** As a 소상공인 고객, I want to 마이비즈데이터를 연동하고 싶다, so that 사업 실적 데이터 기반으로 대출 심사를 받을 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 applicationId와 bizNo가 포함된 요청이 수신되면, THE Wrapper_API SHALL MybizService를 호출하여 해당 bizNo에 대한 마이비즈데이터를 수집한다
2. WHEN MybizService 호출이 성공하면, THE LoanStepService SHALL 해당 LoanApplication의 lastCompletedStep을 MYBIZ_CONNECTED로 업데이트한다
3. WHEN 마이비즈데이터 연동 단계가 완료되면, THE Wrapper_API SHALL 성공 응답(applicationId, completedStep)을 반환한다
4. IF 해당 applicationId의 LoanApplication이 존재하지 않으면, THEN THE Wrapper_API SHALL 404 에러를 반환한다
5. IF 해당 LoanApplication의 status가 DRAFT가 아니면, THEN THE Wrapper_API SHALL 400 에러를 반환한다
6. IF bizNo가 유효하지 않은 형식(10자리 숫자가 아닌 경우)이면, THEN THE Wrapper_API SHALL 400 에러를 반환한다
7. IF 해당 LoanApplication의 lastCompletedStep이 DATA_COLLECTED가 아니면, THEN THE Wrapper_API SHALL 400 에러를 반환하고 단계 순서가 올바르지 않음을 나타내는 에러 메시지를 포함한다
8. IF MybizService 호출이 실패하면, THEN THE Wrapper_API SHALL 502 에러를 반환하고 lastCompletedStep을 변경하지 않는다

### Requirement 6: 래퍼 API 공통 설계 원칙

**User Story:** As a 개발자, I want to 래퍼 API가 일관된 설계 원칙을 따르도록 하고 싶다, so that 코드의 유지보수성과 확장성을 확보할 수 있다.

#### Acceptance Criteria

1. THE Wrapper_API SHALL 각 단계의 범용 서비스 호출과 lastCompletedStep 업데이트를 하나의 트랜잭션으로 처리하며, 범용 서비스 호출 또는 step 업데이트 중 하나라도 실패하면 해당 트랜잭션 전체를 롤백한다
2. THE Wrapper_API SHALL 범용 서비스를 HTTP 재호출이 아닌 서비스 레이어 직접 호출 방식으로 사용한다
3. THE Wrapper_API SHALL 서버 상태가 변경되는 모든 단계에 POST HTTP 메서드를 사용한다
4. THE LoanStepService SHALL 본인 소유 확인(userId와 applicationId 매칭)을 수행한 후 단계를 처리하며, IF userId와 applicationId가 매칭되지 않으면, THEN THE LoanStepService SHALL 403 에러를 반환하고 단계 처리를 수행하지 않는다
5. THE Wrapper_API SHALL 기존 프로젝트의 ControllerDocs 인터페이스 패턴을 따라 Swagger 문서를 분리한다
6. THE Wrapper_API SHALL 기존 프로젝트의 Converter 패턴을 따라 Entity와 DTO 간 변환을 처리한다
7. THE Wrapper_API SHALL 기존 프로젝트의 Service interface + ServiceImpl 패턴을 따른다
8. IF 요청된 단계의 이전 단계가 완료되지 않은 경우(lastCompletedStep이 직전 단계가 아닌 경우), THEN THE LoanStepService SHALL 400 에러를 반환하고 해당 단계를 처리하지 않는다
