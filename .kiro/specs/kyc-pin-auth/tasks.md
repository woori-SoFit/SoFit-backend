# Implementation Plan: KYC & PIN 인증 API

## Overview

SOFIT-21(사업자등록번호 인증)과 SOFIT-22(금융인증서 PIN 인증) 두 API를 sofit-user 모듈의 `domain/auth` 패키지에 구현한다. External Mock 서버와 RestClient로 통신하며, SOFIT-21 브랜치에서 인프라 + KYC 인증을 완료한 후, SOFIT-22 브랜치에서 PIN 인증을 추가 구현한다.

---

## Part A: SOFIT-21 사업자등록번호 인증

**브랜치:** `feat/SOFIT-21-business-verification`
**PR 베이스:** `dev`

### Tasks

- [ ] 1. Phase 1: 인프라/설정 (환경변수, RestClient, SecurityConfig)
  - [ ] 1.1 application.yml에 External Mock URL 환경변수 추가 및 .env.example 업데이트
    - `sofit-user/src/main/resources/application.yml`에 `external.mock.url` 설정 추가
    - `.env.example`에 `EXTERNAL_MOCK_URL` 항목 추가
    - _Requirements: 3.1_

  - [ ] 1.2 ExternalMockConfig 생성 (RestClient Bean 등록)
    - `sofit-user/src/main/java/com/sofit/user/global/config/ExternalMockConfig.java` 생성
    - `@Value("${external.mock.url}")` 로 base URL 주입
    - RestClient Bean 등록 (`RestClient.builder().baseUrl(baseUrl).build()`)
    - _Requirements: 3.1, 3.2_

  - [ ] 1.3 SecurityConfig 생성 (permitAll, 세션 정책)
    - `sofit-user/src/main/java/com/sofit/user/global/config/SecurityConfig.java` 생성
    - `/api/auth/**` 경로 permitAll 설정
    - 세션 생성 정책: `SessionCreationPolicy.IF_REQUIRED`
    - _Requirements: 4.1, 4.2_

- [ ] 2. Phase 2: Entity 확장 (BusinessProfile 필드 추가, Enum)
  - [ ] 2.1 BusinessProfileStatus enum 생성 (sofit-common)
    - `sofit-common/src/main/java/com/sofit/common/entity/enums/BusinessProfileStatus.java` 생성
    - PENDING, VERIFIED, FAILED 값 정의
    - common에 위치하여 멀티모듈에서 공유 가능
    - _Requirements: 1.3_

  - [ ] 2.2 BusinessProfile 엔티티 확장 (sofit-common)
    - `sofit-common/src/main/java/com/sofit/common/entity/BusinessProfile.java` 수정
    - User와 @ManyToOne 관계 추가 (user_id FK)
    - businessNumber, representativeName, businessCategory, businessType, businessName, businessAddress, openDate 필드 추가
    - status (BusinessProfileStatus enum), verifiedAt 필드 추가
    - isMybizConnected, mybizConnectedAt, mydataAllAgreed, mydataAllAgreedAt 필드 추가
    - `createVerified(User, ExternalKycResponse)` 정적 팩토리 메서드 추가
    - _Requirements: 1.3, 1.7_

- [ ] 3. Phase 3: Exception/Code 정의
  - [ ] 3.1 AuthErrorCode enum 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/auth/exception/AuthErrorCode.java` 생성
    - BaseErrorCode 인터페이스 구현
    - AUTH4004 (404): "일치하는 사업자등록번호를 찾을 수 없습니다."
    - AUTH4006 (400): "입력값 형식이 올바르지 않습니다."
    - AUTH5001 (502): "외부 인증 서버와 통신 중 오류가 발생했습니다."
    - _Requirements: 5.1, 5.3_

  - [ ] 3.2 AuthSuccessCode enum 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/auth/exception/AuthSuccessCode.java` 생성
    - BaseSuccessCode 인터페이스 구현
    - AUTH2001 (200): "사업자 인증에 성공했습니다."
    - _Requirements: 5.2_

- [ ] 4. Phase 4: DTO 생성 (KYC 관련)
  - [ ] 4.1 KYC Request DTO 생성
    - `domain/auth/dto/request/BusinessVerificationRequest.java` — record, @NotBlank businessNumber (하이픈 없는 10자리)
    - `domain/auth/dto/request/ExternalKycRequest.java` — record, businessNumber
    - _Requirements: 1.1, 1.6_

  - [ ] 4.2 KYC Response DTO 생성
    - `domain/auth/dto/response/BusinessVerificationResponse.java` — record (kycId, businessNumber, representativeName, businessName, businessType, openDate, isValid, verifiedAt)
    - `domain/auth/dto/response/ExternalKycResponse.java` — record (businessNumber, representativeName, businessCategory, businessType, businessName, businessAddress, openDate, status)
    - _Requirements: 1.4_

- [ ] 5. Phase 5: ExternalMockClient 구현 (KYC)
  - [ ] 5.1 ExternalMockClient 생성 및 KYC 호출 메서드 구현
    - `sofit-user/src/main/java/com/sofit/user/domain/auth/service/ExternalMockClient.java` 생성
    - RestClient 주입 (ExternalMockConfig에서 등록한 Bean 사용)
    - `callKycVerify(String businessNumber)` → POST `/ext/kyc/verify`
    - RestClientException catch → `throw new BaseException(AuthErrorCode.AUTH5001)`
    - _Requirements: 1.2, 3.3_

- [ ] 6. Phase 6: Service 구현 (KYC)
  - [ ] 6.1 AuthService 인터페이스 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/auth/service/AuthService.java` 생성
    - `verifyBusiness(BusinessVerificationRequest request, Long userId)` 메서드 정의
    - _Requirements: 1.1_

  - [ ] 6.2 AuthServiceImpl 구현 (KYC 인증 로직)
    - `sofit-user/src/main/java/com/sofit/user/domain/auth/service/AuthServiceImpl.java` 생성
    - verifyBusiness: ExternalMockClient.callKycVerify 호출 → status 확인 → ACTIVE이면 BusinessProfile.createVerified() 저장 → BusinessVerificationResponse 반환
    - CLOSED/NOT_FOUND이면 `throw new BaseException(AuthErrorCode.AUTH4004)`
    - _Requirements: 1.2, 1.3, 1.5_

- [ ] 7. Phase 7: Controller 구현 (KYC)
  - [ ] 7.1 AuthController 생성 및 KYC 엔드포인트 구현
    - `sofit-user/src/main/java/com/sofit/user/domain/auth/controller/AuthController.java` 생성
    - `POST /api/auth/signup/business-verification` → @Valid + @RequestBody BusinessVerificationRequest → AuthService.verifyBusiness 호출 → ApiResponse.onSuccess(AUTH2001, result)
    - _Requirements: 1.1, 1.4_

- [ ] 8. Checkpoint - SOFIT-21 컴파일 확인
  - Ensure all tests pass, ask the user if questions arise.
  - `./gradlew :sofit-common:compileJava` 실행하여 common 모듈 컴파일 확인
  - `./gradlew :sofit-user:compileJava` 실행하여 user 모듈 컴파일 확인

---

## Part B: SOFIT-22 금융인증서 PIN 인증

**브랜치:** `feat/SOFIT-22-financial-cert-pin`
**PR 베이스:** `dev` (SOFIT-21 머지 후)

### Tasks

- [ ] 9. Phase 8: Exception/Code 추가 (PIN 관련)
  - [ ] 9.1 AuthErrorCode에 PIN 관련 에러 코드 추가
    - `AuthErrorCode.java` 수정
    - AUTH4001 (401): "PIN 번호가 올바르지 않습니다."
    - AUTH4005 (404): "등록된 금융인증서를 찾을 수 없습니다."
    - _Requirements: 5.1_

  - [ ] 9.2 AuthSuccessCode에 PIN 성공 코드 추가
    - `AuthSuccessCode.java` 수정
    - AUTH2006 (200): "금융인증서 본인인증에 성공했습니다."
    - _Requirements: 5.2_

- [ ] 10. Phase 9: DTO 생성 (PIN 관련)
  - [ ] 10.1 PIN Request DTO 생성
    - `domain/auth/dto/request/FinancialCertVerifyRequest.java` — record, @NotBlank phoneNumber (하이픈 없는 11자리), @NotBlank pin (6자리)
    - `domain/auth/dto/request/ExternalFinancialCertRequest.java` — record, phoneNumber, pin
    - _Requirements: 2.1, 2.7, 2.8_

  - [ ] 10.2 PIN Response DTO 생성
    - `domain/auth/dto/response/FinancialCertVerifyResponse.java` — record (certId, certNumber, holderName, phoneNumber, status, verifiedAt)
    - `domain/auth/dto/response/ExternalFinancialCertResponse.java` — record (certId, certNumber, holderName, phoneNumber, status, verifiedAt)
    - _Requirements: 2.3_

- [ ] 11. Phase 10: ExternalMockClient에 PIN 호출 메서드 추가
  - [ ] 11.1 ExternalMockClient에 callFinancialCertVerify 메서드 추가
    - `ExternalMockClient.java` 수정
    - `callFinancialCertVerify(String phoneNumber, String pin)` → POST `/ext/financial-certs/verify`
    - RestClientException catch → `throw new BaseException(AuthErrorCode.AUTH5001)`
    - _Requirements: 2.2, 3.3_

- [ ] 12. Phase 11: Service 확장 (PIN 인증)
  - [ ] 12.1 AuthService 인터페이스에 PIN 메서드 추가
    - `AuthService.java` 수정
    - `verifyFinancialCertificate(FinancialCertVerifyRequest request)` 메서드 추가
    - _Requirements: 2.1_

  - [ ] 12.2 AuthServiceImpl에 PIN 인증 로직 추가
    - `AuthServiceImpl.java` 수정
    - verifyFinancialCertificate: ExternalMockClient.callFinancialCertVerify 호출 → status 확인
    - VERIFIED이면 FinancialCertVerifyResponse 반환
    - PIN_MISMATCH이면 `throw new BaseException(AuthErrorCode.AUTH4001)`
    - NOT_FOUND이면 `throw new BaseException(AuthErrorCode.AUTH4005)`
    - DB 저장 없음
    - _Requirements: 2.2, 2.3, 2.4, 2.5, 2.6_

- [ ] 13. Phase 12: Controller 확장 (PIN 엔드포인트)
  - [ ] 13.1 AuthController에 PIN 인증 엔드포인트 추가
    - `AuthController.java` 수정
    - `POST /api/auth/financial-certificate/verify` → @Valid + @RequestBody FinancialCertVerifyRequest → AuthService.verifyFinancialCertificate 호출 → ApiResponse.onSuccess(AUTH2006, result)
    - _Requirements: 2.1, 2.3_

- [ ] 14. Checkpoint - SOFIT-22 컴파일 확인
  - Ensure all tests pass, ask the user if questions arise.
  - `./gradlew :sofit-user:compileJava` 실행하여 컴파일 확인

---

## Notes

- 각 Phase 완료 후 컴파일 확인을 권장합니다.
- BusinessProfile, BusinessProfileStatus는 sofit-common에 위치하여 멀티모듈에서 공유합니다.
- SOFIT-22는 SOFIT-21이 dev에 머지된 후 별도 브랜치에서 작업합니다.

---

## Git 안내

### SOFIT-21 (사업자등록번호 인증)

**브랜치:**
```
feat/SOFIT-21-business-verification
```

**커밋 메시지 (Phase별):**
```
[SOFIT-21] Feat: 인프라 설정 (ExternalMockConfig, SecurityConfig)
[SOFIT-21] Feat: BusinessProfile 엔티티 확장 및 BusinessProfileStatus enum 추가
[SOFIT-21] Feat: AuthErrorCode, AuthSuccessCode 정의
[SOFIT-21] Feat: KYC 인증 DTO 생성
[SOFIT-21] Feat: ExternalMockClient KYC 호출 구현
[SOFIT-21] Feat: AuthService KYC 인증 로직 구현
[SOFIT-21] Feat: AuthController KYC 엔드포인트 구현
```

### SOFIT-22 (금융인증서 PIN 인증)

**브랜치:**
```
feat/SOFIT-22-financial-cert-pin
```

**커밋 메시지 (Phase별):**
```
[SOFIT-22] Feat: AuthErrorCode, AuthSuccessCode PIN 관련 코드 추가
[SOFIT-22] Feat: PIN 인증 DTO 생성
[SOFIT-22] Feat: ExternalMockClient PIN 호출 메서드 추가
[SOFIT-22] Feat: AuthService PIN 인증 로직 구현
[SOFIT-22] Feat: AuthController PIN 인증 엔드포인트 추가
```

---

## PR 본문

### SOFIT-21 PR

```
## 기능 설명
사업자등록번호 인증(KYC) API 구현

## 작업 상세 내용
- [x] ExternalMockConfig (RestClient Bean), SecurityConfig 인프라 설정
- [x] BusinessProfile 엔티티 확장 (sofit-common)
- [x] BusinessProfileStatus enum 추가 (sofit-common)
- [x] AuthErrorCode, AuthSuccessCode 정의
- [x] KYC Request/Response DTO 생성 (4개 파일)
- [x] ExternalMockClient KYC 호출 구현
- [x] AuthService / AuthServiceImpl KYC 인증 로직 구현
- [x] AuthController KYC 엔드포인트 구현

## 확인한 내용
- [ ] 로컬 실행 확인
- [ ] 주요 기능 동작 확인
- [ ] 에러 로그 확인
- [ ] 기존 기능 영향 여부 확인

## 스크린샷 / 실행 결과

## 기타 공유사항
- External Mock 서버 URL은 환경변수(EXTERNAL_MOCK_URL)로 관리
- BusinessProfile은 sofit-common에 위치 (멀티모듈 공유)

## 관련 티켓
- closes #SOFIT-21
```

### SOFIT-22 PR

```
## 기능 설명
금융인증서 PIN 인증 API 구현

## 작업 상세 내용
- [x] AuthErrorCode, AuthSuccessCode에 PIN 관련 코드 추가
- [x] PIN Request/Response DTO 생성 (4개 파일)
- [x] ExternalMockClient PIN 호출 메서드 추가
- [x] AuthServiceImpl PIN 인증 로직 추가
- [x] AuthController PIN 인증 엔드포인트 추가

## 확인한 내용
- [ ] 로컬 실행 확인
- [ ] 주요 기능 동작 확인
- [ ] 에러 로그 확인
- [ ] 기존 기능 영향 여부 확인

## 스크린샷 / 실행 결과

## 기타 공유사항
- PIN 인증은 DB 저장 없이 External Mock 응답을 그대로 전달
- SOFIT-21 머지 후 작업

## 관련 티켓
- closes #SOFIT-22
```

---

## API Dog 테스트 JSON

### 사업자등록번호 인증 (POST /api/auth/signup/business-verification)

**성공 케이스:**
```json
{
  "businessNumber": "1234567890"
}
```

**실패 케이스 (형식 오류):**
```json
{
  "businessNumber": "123-45-67890"
}
```

**실패 케이스 (빈 값):**
```json
{
  "businessNumber": ""
}
```

### 금융인증서 PIN 인증 (POST /api/auth/financial-certificate/verify)

**성공 케이스:**
```json
{
  "phoneNumber": "01012345678",
  "pin": "123456"
}
```

**실패 케이스 (PIN 불일치):**
```json
{
  "phoneNumber": "01012345678",
  "pin": "000000"
}
```

**실패 케이스 (형식 오류):**
```json
{
  "phoneNumber": "010-1234-5678",
  "pin": "12345"
}
```

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "2.1", "3.1", "3.2"] },
    { "id": 1, "tasks": ["2.2", "4.1", "4.2"] },
    { "id": 2, "tasks": ["5.1", "6.1"] },
    { "id": 3, "tasks": ["6.2"] },
    { "id": 4, "tasks": ["7.1"] },
    { "id": 5, "tasks": ["9.1", "9.2", "10.1", "10.2"] },
    { "id": 6, "tasks": ["11.1", "12.1"] },
    { "id": 7, "tasks": ["12.2"] },
    { "id": 8, "tasks": ["13.1"] }
  ]
}
```
