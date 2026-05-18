# Requirements Document

## Introduction

사용자가 대출 실행 계좌를 등록할 때, 본인 명의 계좌인지 확인하기 위해 코데프(CODEF) API를 활용한 1원 이체 방식의 계좌 인증 기능을 구현한다. 인증 흐름은 1원 송금 요청과 인증코드 확인의 두 단계로 구성되며, Redis를 활용하여 인증코드를 임시 저장하고 TTL 기반으로 만료를 관리한다.

## Glossary

- **Account_Verification_Service**: 1원 이체 기반 계좌 인증을 처리하는 서버 측 서비스 컴포넌트
- **CODEF_Client**: 코데프 데모 서버(https://development.codef.io)와 HTTP 통신을 담당하는 클라이언트 컴포넌트
- **Redis_Store**: 인증코드를 TTL 기반으로 임시 저장하는 Redis 저장소
- **Rate_Limiter**: 계좌번호당 일일 요청 횟수를 제한하는 컴포넌트
- **Auth_Code**: 코데프 API가 반환하는 4자리 숫자 인증코드 (입금자명에 포함)
- **Verification_Code**: 사용자가 통장에서 확인하여 입력하는 4자리 숫자
- **Application_ID**: 대출 신청 식별자 (현재 하드코딩 또는 Request Body로 전달)
- **Loan_Execution**: 대출 실행 정보를 저장하는 테이블 (account_number, bank_code 포함)

## Requirements

### Requirement 1: 1원 송금 요청 API

**User Story:** As a 대출 신청자, I want 내 계좌로 1원 송금을 요청하여 인증코드를 받을 수 있도록, so that 본인 명의 계좌임을 증명할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 bankCode와 accountNumber를 포함한 POST 요청을 /api/account-verification 엔드포인트로 전송하면, THE Account_Verification_Service SHALL bankCode가 빈 문자열이 아닌 문자열이고 accountNumber가 하이픈 없이 숫자로만 구성된 7~20자리 문자열인지 검증한 후, 코데프 API를 호출하여 해당 계좌로 1원 송금을 요청한다.
2. WHEN 코데프 API가 authCode를 정상 반환하면, THE Account_Verification_Service SHALL bankName, maskedAccountNumber, accountHolder, expiredAt을 포함한 성공 응답을 반환한다.
3. WHEN 코데프 API 호출이 성공하면, THE Redis_Store SHALL authCode를 applicationId를 키로 하여 TTL 300초(5분)로 저장한다.
4. WHEN 1원 송금 요청이 성공하면, THE Account_Verification_Service SHALL 현재 시각으로부터 5분 후의 시각을 ISO 8601 형식(yyyy-MM-dd'T'HH:mm:ss)으로 expiredAt 필드에 계산하여 응답에 포함한다.
5. IF bankCode 또는 accountNumber가 유효성 검증에 실패하면, THEN THE Account_Verification_Service SHALL 요청을 거부하고 유효하지 않은 계좌번호임을 나타내는 에러 응답을 반환한다.
6. IF 코데프 API 호출이 실패하거나 30초 이내에 응답하지 않으면, THEN THE Account_Verification_Service SHALL 계좌 인증 서비스 일시 오류를 나타내는 에러 응답을 반환한다.
7. IF 해당 계좌번호의 일일 요청 횟수가 5회를 초과하면, THEN THE Account_Verification_Service SHALL 코데프 API를 호출하지 않고 일일 요청 한도 초과를 나타내는 에러 응답을 반환한다.

### Requirement 2: 코데프 API 연동

**User Story:** As a 시스템 운영자, I want 코데프 데모 서버와 안정적으로 통신할 수 있도록, so that 1원 이체 인증 기능이 정상 동작한다.

#### Acceptance Criteria

1. THE CODEF_Client SHALL 코데프 데모 서버(https://development.codef.io/v1/kr/bank/a/account/transfer-authentication)로 POST 요청을 전송한다.
2. THE CODEF_Client SHALL 요청 시 organization(은행코드), account(계좌번호), inPrintType("0"), inPrintContent("")를 포함한 JSON Body를 Content-Type: application/json 헤더와 함께 전송한다.
3. THE CODEF_Client SHALL 연결 타임아웃과 읽기 타임아웃을 각각 30초로 설정한다.
4. WHEN 코데프 API가 정상 응답(HTTP 200)을 반환하면, THE CODEF_Client SHALL 응답 JSON에서 authCode(4자리 숫자 문자열)를 추출하여 반환한다.
5. IF 코데프 API가 연결 실패, 타임아웃, 또는 HTTP 4xx/5xx 응답을 반환하면, THEN THE CODEF_Client SHALL 재시도 없이 즉시 ACCOUNT5001 에러코드와 함께 "계좌 인증 서비스에 일시적인 오류가 발생했습니다." 메시지를 반환한다.

### Requirement 3: 일일 요청 횟수 제한

**User Story:** As a 시스템 운영자, I want 계좌번호당 1일 5회로 요청을 제한할 수 있도록, so that 코데프 API 남용을 방지한다.

#### Acceptance Criteria

1. WHEN 1원 송금 요청이 수신되면, THE Rate_Limiter SHALL 해당 계좌번호의 당일(KST 00:00:00~23:59:59 기준) 요청 횟수를 조회하여 5회 미만인 경우에만 코데프 API 호출을 허용한다.
2. IF 해당 계좌번호의 당일 요청 횟수가 5회에 도달한 상태이면, THEN THE Rate_Limiter SHALL ACCOUNT4002 에러코드와 함께 요청을 거부하고 코데프 API를 호출하지 않는다.
3. WHEN 코데프 API로부터 정상 응답을 수신하면, THE Rate_Limiter SHALL 해당 계좌번호의 당일 요청 횟수를 1 증가시킨다.
4. IF 코데프 API 호출이 실패하면(ACCOUNT5001 등 외부 오류), THEN THE Rate_Limiter SHALL 해당 계좌번호의 당일 요청 횟수를 증가시키지 않는다.
5. WHEN KST 기준 자정(00:00:00)이 도래하면, THE Rate_Limiter SHALL 모든 계좌번호의 요청 횟수 카운터를 0으로 초기화한다.

### Requirement 4: 입력값 검증

**User Story:** As a 대출 신청자, I want 잘못된 계좌 정보 입력 시 명확한 오류 메시지를 받을 수 있도록, so that 올바른 정보를 다시 입력할 수 있다.

#### Acceptance Criteria

1. IF bankCode가 빈 값이거나 null이면, THEN THE Account_Verification_Service SHALL isSuccess: false와 함께 필수 입력값 누락을 나타내는 에러 응답을 반환한다.
2. IF accountNumber가 빈 값이거나 null이면, THEN THE Account_Verification_Service SHALL isSuccess: false와 함께 필수 입력값 누락을 나타내는 에러 응답을 반환한다.
3. IF accountNumber가 숫자가 아닌 문자를 포함하거나 7자리 미만 또는 20자리 초과이면, THEN THE Account_Verification_Service SHALL isSuccess: false와 함께 계좌번호 형식 오류를 나타내는 에러 응답을 반환한다.
4. IF verificationCode가 정확히 4자리 숫자 형식이 아니면, THEN THE Account_Verification_Service SHALL isSuccess: false와 함께 인증번호 형식 오류를 나타내는 에러 응답을 반환한다.
5. IF bankCode가 시스템에 등록된 은행 기관코드 목록에 존재하지 않으면, THEN THE Account_Verification_Service SHALL isSuccess: false와 함께 유효하지 않은 은행코드를 나타내는 에러 응답을 반환한다.

### Requirement 5: 1원 인증 확인 API

**User Story:** As a 대출 신청자, I want 통장에서 확인한 인증코드를 입력하여 계좌 인증을 완료할 수 있도록, so that 대출 실행 계좌를 등록할 수 있다.

#### Acceptance Criteria

1. WHEN 사용자가 4자리 숫자로 구성된 verificationCode를 포함한 POST 요청을 /api/account-verification/confirm 엔드포인트로 전송하면, THE Account_Verification_Service SHALL applicationId로 Redis에 저장된 authCode를 조회한다.
2. WHEN verificationCode와 Redis에 저장된 authCode가 일치하면, THE Account_Verification_Service SHALL accountVerified를 true로 설정한 성공 응답을 반환한다.
3. WHEN 인증이 성공하면, THE Account_Verification_Service SHALL Redis에서 해당 applicationId의 인증코드를 삭제한다.
4. WHEN 인증이 성공하면, THE Account_Verification_Service SHALL loan_execution 테이블에 account_number와 bank_code를 저장한다.
5. IF verificationCode와 Redis에 저장된 authCode가 일치하지 않으면, THEN THE Account_Verification_Service SHALL ACCOUNT4003 에러코드와 함께 인증번호 불일치를 나타내는 에러 응답을 반환한다.
6. IF Redis에서 applicationId에 해당하는 authCode가 존재하지 않으면(TTL 5분 만료), THEN THE Account_Verification_Service SHALL ACCOUNT4004 에러코드와 함께 인증 시간 만료를 나타내는 에러 응답을 반환한다.
7. IF verificationCode가 빈 값이거나 4자리 숫자 형식이 아닌 경우, THEN THE Account_Verification_Service SHALL 잘못된 요청임을 나타내는 에러 응답을 반환한다.

### Requirement 6: 인증 만료 처리

**User Story:** As a 시스템 운영자, I want 인증코드가 5분 후 자동 만료되도록, so that 보안을 유지할 수 있다.

#### Acceptance Criteria

1. WHEN 코데프 API 응답으로 authCode를 수신하면, THE Redis_Store SHALL 해당 authCode를 applicationId를 키로 하여 TTL 300초(5분)로 저장한다.
2. IF 인증 확인 요청 시 Redis에서 applicationId에 해당하는 authCode가 존재하지 않으면(TTL 만료), THEN THE Account_Verification_Service SHALL ACCOUNT4004 에러코드와 함께 인증 시간 만료를 나타내는 메시지를 반환한다.
3. WHEN 인증코드 검증이 성공하면, THE Redis_Store SHALL 해당 applicationId의 authCode를 즉시 삭제하여 동일 인증코드의 재사용을 방지한다.
4. IF 인증코드가 불일치하면, THEN THE Account_Verification_Service SHALL ACCOUNT4003 에러코드와 함께 인증번호 불일치를 나타내는 메시지를 반환하고, 기존 TTL을 리셋하지 않고 유지한다.

### Requirement 7: 계좌번호 마스킹

**User Story:** As a 대출 신청자, I want 응답에서 계좌번호가 마스킹 처리되어 표시되도록, so that 개인정보가 보호된다.

#### Acceptance Criteria

1. WHEN 1원 송금 요청이 성공하면, THE Account_Verification_Service SHALL 계좌번호의 5번째 자리부터 8번째 자리까지 4자리를 별표(*)로 대체하여 maskedAccountNumber 필드에 포함한다.
2. THE Account_Verification_Service SHALL 마스킹된 계좌번호를 "{앞4자리}-****-{9번째 자리부터 끝까지}" 형식으로 반환한다.
3. IF 계좌번호의 총 자릿수가 9자리 미만이면, THEN THE Account_Verification_Service SHALL 마스킹을 적용하지 않고 계좌 인증 요청을 유효하지 않은 계좌번호 오류로 거부한다.
4. THE Account_Verification_Service SHALL 1원 송금 요청 응답에서 원본 계좌번호를 어떠한 필드에도 포함하지 않고, maskedAccountNumber 필드만 반환한다.

### Requirement 8: 공통 응답 포맷 준수

**User Story:** As a 프론트엔드 개발자, I want 계좌 인증 API가 프로젝트 공통 응답 포맷을 따르도록, so that 일관된 방식으로 응답을 처리할 수 있다.

#### Acceptance Criteria

1. THE Account_Verification_Service SHALL 모든 성공 응답을 ApiResponse 포맷(isSuccess: true, code, message, result)으로 반환하며, HTTP 상태 코드는 해당 BaseSuccessCode에 정의된 HttpStatus를 따른다.
2. THE Account_Verification_Service SHALL 모든 실패 응답을 ApiResponse 포맷(isSuccess: false, code, message)으로 반환하며, result 필드는 포함하지 않고, HTTP 상태 코드는 해당 BaseErrorCode에 정의된 HttpStatus를 따른다.
3. IF 도메인 예외가 발생하면, THEN THE Account_Verification_Service SHALL BaseException을 throw하여 GlobalExceptionHandler가 해당 ErrorCode의 code, message, httpStatus를 사용해 ApiResponse 실패 응답을 반환하도록 한다.
4. IF 요청 파라미터 검증(@Valid)이 실패하면, THEN THE Account_Verification_Service SHALL GlobalExceptionHandler를 통해 COMMON4000 코드와 HTTP 400 상태로 ApiResponse 실패 응답을 반환한다.
5. IF 예상치 못한 예외(non-BaseException)가 발생하면, THEN THE Account_Verification_Service SHALL GlobalExceptionHandler를 통해 COMMON5000 코드와 HTTP 500 상태로 ApiResponse 실패 응답을 반환한다.

### Requirement 9: 임시 인증 컨텍스트 관리

**User Story:** As a 개발자, I want 세션/로그인 미구현 상태에서도 인증 흐름이 동작하도록, so that 계좌 인증 기능을 독립적으로 개발하고 테스트할 수 있다.

#### Acceptance Criteria

1. THE Account_Verification_Service SHALL 세션 기반 userId 추출을 대체하여 Long 타입의 하드코딩된 userId 값(예: 1L)을 사용한다.
2. THE Account_Verification_Service SHALL applicationId를 Request Body를 통해 전달받아 처리하며, Request Body에 applicationId가 포함되지 않은 경우 하드코딩된 기본값(예: 1L)을 사용한다.
3. WHEN 1원 송금 요청이 수신되면, THE Account_Verification_Service SHALL authCode와 함께 bankCode, accountNumber를 Redis에 동일한 키(applicationId) 하위에 저장하고 TTL을 300초로 설정한다.
4. WHEN 인증 확인 성공 시, THE Account_Verification_Service SHALL Redis에 저장된 bankCode와 accountNumber를 조회하여 loan_execution 테이블의 bank_code, account_number 컬럼에 저장한다.
