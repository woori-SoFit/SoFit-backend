# Requirements Document

## Introduction

SoFit 회원가입 플로우에서 사용되는 두 가지 인증 API를 구현한다.
1. **SOFIT-21**: 사업자등록번호 인증 API — External Mock 서버를 통해 사업자 진위를 확인하고 결과를 DB에 저장한다.
2. **SOFIT-22**: 금융인증서 PIN 인증 API — External Mock 서버를 통해 금융인증서 PIN 본인인증을 수행한다 (DB 저장 없음).

두 API 모두 비로그인 상태에서 임시 세션을 사용하며, sofit-user 모듈의 `domain/auth` 패키지에 구현한다.

## Glossary

- **Auth_System**: sofit-user 모듈의 auth 도메인에 위치한 인증 처리 시스템
- **External_Mock**: 국세청 KYC, 금융인증서 등 외부 API를 모사하는 Mock 서버
- **BusinessProfile**: business_profiles 테이블에 매핑되는 사업자 프로필 엔티티
- **KYC_Verification**: 사업자등록번호 진위 확인 프로세스
- **PIN_Verification**: 금융인증서 PIN 본인인증 프로세스
- **ApiResponse**: 프로젝트 공통 응답 포맷 (`isSuccess`, `code`, `message`, `result`)
- **RestTemplate**: External Mock 서버 호출에 사용하는 HTTP 클라이언트
- **AuthErrorCode**: auth 도메인 전용 에러 코드 enum

## Requirements

### Requirement 1: 사업자등록번호 인증 API

**User Story:** As a 소상공인(USER), I want 사업자등록번호를 입력하여 사업자 진위를 확인받고 싶다, so that 회원가입 절차를 진행할 수 있다.

#### Acceptance Criteria

1. WHEN 클라이언트가 POST `/api/auth/signup/business-verification` 요청을 전송하면, THE Auth_System SHALL `businessNumber` 필드를 포함한 요청 본문을 수신하여 처리한다.
2. WHEN 사업자등록번호 인증 요청을 수신하면, THE Auth_System SHALL External Mock 서버의 POST `/ext/kyc/verify` 엔드포인트를 RestTemplate으로 호출하여 사업자 진위를 확인한다.
3. WHEN External Mock 서버가 유효한 사업자 정보를 반환하면, THE Auth_System SHALL business_profiles 테이블에 사업자 프로필을 저장하고 status를 VERIFIED로 설정한다.
4. WHEN 사업자 인증이 성공하면, THE Auth_System SHALL 코드 `AUTH2001`, 메시지 "사업자 인증에 성공했습니다."와 함께 인증 결과(kycId, businessNumber, representativeName, businessName, businessType, openDate, isValid, verifiedAt)를 ApiResponse 형식으로 반환한다.
5. IF External Mock 서버가 폐업 또는 미등록 사업자로 응답하면, THEN THE Auth_System SHALL HTTP 404 상태와 코드 `AUTH4004`, 메시지 "일치하는 사업자등록번호를 찾을 수 없습니다."를 ApiResponse 형식으로 반환한다.
6. THE Auth_System SHALL `businessNumber` 요청 필드를 하이픈 없는 10자리 숫자 형식으로 검증한다.
7. WHEN 사업자 프로필을 저장할 때, THE Auth_System SHALL user_id를 FK로 설정하여 User 엔티티와 N:1 관계를 유지한다.

### Requirement 2: 금융인증서 PIN 인증 API

**User Story:** As a 소상공인(USER), I want 금융인증서 PIN을 입력하여 본인인증을 수행하고 싶다, so that 회원가입 및 주요 거래 시 본인 확인을 완료할 수 있다.

#### Acceptance Criteria

1. WHEN 클라이언트가 POST `/api/auth/financial-certificate/verify` 요청을 전송하면, THE Auth_System SHALL `phoneNumber`와 `pin` 필드를 포함한 요청 본문을 수신하여 처리한다.
2. WHEN PIN 인증 요청을 수신하면, THE Auth_System SHALL External Mock 서버의 POST `/ext/financial-certs/verify` 엔드포인트를 RestTemplate으로 호출하여 PIN 인증을 수행한다.
3. WHEN External Mock 서버가 인증 성공을 반환하면, THE Auth_System SHALL 코드 `AUTH2006`, 메시지 "금융인증서 본인인증에 성공했습니다."와 함께 인증 결과(certId, certNumber, holderName, phoneNumber, status, verifiedAt)를 ApiResponse 형식으로 반환한다.
4. IF External Mock 서버가 PIN 불일치로 응답하면, THEN THE Auth_System SHALL 코드 `AUTH4001`, 메시지 "PIN 번호가 올바르지 않습니다."를 ApiResponse 형식으로 반환한다.
5. IF External Mock 서버가 등록된 금융인증서 없음으로 응답하면, THEN THE Auth_System SHALL 코드 `AUTH4005`, 메시지 "등록된 금융인증서를 찾을 수 없습니다."를 ApiResponse 형식으로 반환한다.
6. THE Auth_System SHALL PIN 인증 결과를 DB에 저장하지 않고 External Mock 응답을 그대로 클라이언트에 전달한다.
7. THE Auth_System SHALL `phoneNumber` 요청 필드를 하이픈 없는 11자리 숫자 형식으로 검증한다.
8. THE Auth_System SHALL `pin` 요청 필드를 6자리 숫자 형식으로 검증한다.

### Requirement 3: External Mock 통신 설정

**User Story:** As a 개발자, I want External Mock 서버 URL을 환경변수로 관리하고 싶다, so that 환경별로 유연하게 Mock 서버 주소를 변경할 수 있다.

#### Acceptance Criteria

1. THE Auth_System SHALL External Mock 서버의 base URL을 환경변수(`EXTERNAL_MOCK_URL`)로 관리한다.
2. THE Auth_System SHALL RestTemplate Bean을 설정하여 External Mock 서버와의 HTTP 통신에 사용한다.
3. IF External Mock 서버 호출 중 통신 오류가 발생하면, THEN THE Auth_System SHALL 적절한 에러 코드와 메시지를 포함한 ApiResponse를 반환한다.

### Requirement 4: 보안 설정

**User Story:** As a 개발자, I want 인증 API 엔드포인트를 비로그인 상태에서 접근 가능하도록 설정하고 싶다, so that 회원가입 플로우에서 로그인 없이 인증을 수행할 수 있다.

#### Acceptance Criteria

1. THE Auth_System SHALL `/api/auth/**` 경로를 Spring Security에서 permitAll로 설정한다.
2. THE Auth_System SHALL 인증 API 요청 시 임시 세션을 생성하여 회원가입 플로우 상태를 유지한다.

### Requirement 5: 예외 처리 체계

**User Story:** As a 개발자, I want auth 도메인 전용 에러 코드를 정의하고 싶다, so that 인증 관련 오류를 명확하게 구분하여 클라이언트에 전달할 수 있다.

#### Acceptance Criteria

1. THE Auth_System SHALL auth 도메인 전용 AuthErrorCode enum을 정의하여 BaseErrorCode 인터페이스를 구현한다.
2. THE Auth_System SHALL auth 도메인 전용 AuthSuccessCode enum을 정의하여 BaseSuccessCode 인터페이스를 구현한다.
3. WHEN 인증 관련 예외가 발생하면, THE Auth_System SHALL BaseException을 통해 AuthErrorCode를 전달하고 GlobalExceptionHandler에서 ApiResponse 형식으로 변환한다.
