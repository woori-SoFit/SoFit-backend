# Implementation Plan: 인증 및 사용자 관리

## Overview

SoFit 대출 플랫폼의 인증 및 사용자 관리 기능을 Phase별로 구현한다. 기존 kyc-pin-auth 스펙에서 구현된 코드를 멀티스텝 회원가입 플로우에 맞게 수정하고, 로그인/로그아웃/회원탈퇴 기능을 추가한다.

**브랜치**: `feat/SOFIT-20-auth-user-management`
**PR 베이스**: `dev`

## Tasks

- [x] 1. Phase 1: 공통 인프라 — User 엔티티 확장, UserRole 수정, Redis 세션 설정, SecurityConfig [SOFIT-20]
  - 모든 기능의 기반이 되는 공통 인프라를 먼저 구성한다.
  - 커밋: `[SOFIT-20] Feat: 공통 인프라 구성 (User 엔티티, Redis 세션, SecurityConfig)`

  - [x] 1.1 User 엔티티 확장 및 UserRole enum 수정
    - `sofit-common`의 User.java에 ERD 컬럼 추가: loginId, passwordHash, name, phoneNumber, residentNumber, role, status, inactivatedAt
    - UserRole enum을 USER, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV로 변경
    - UserStatus enum 신규 생성 (ACTIVE, INACTIVE)
    - User에 정적 팩토리 메서드 `createUser(...)` 및 `inactivate()` 메서드 추가
    - `sofit-common`에 UserRepository.java 생성 (findByLoginId, findByBusinessNumber 등)
    - _Requirements: 4.2, 4.3, 4.11, 6.1, 8.1, 8.3_

  - [x] 1.2 RegistrationProcess 엔티티 신규 생성 및 RegistrationStep enum 생성
    - `sofit-common`에 RegistrationProcess.java 신규 생성: registration_process_id, registration_id, user_id, step, business_number, business_name, representative_name, open_date, business_type, pin_verified, pin_verified_at
    - RegistrationStep enum 생성 (STEP_1_COMPLETED, STEP_2_COMPLETED, COMPLETED, EXPIRED)
    - RegistrationProcess에 팩토리 메서드 `createForStep1(...)`, `completeStep2(...)`, `completeRegistration(...)`, `expire()`, `isExpired()` 추가
    - `sofit-common`에 RegistrationProcessRepository.java 생성 (findByRegistrationId 메서드 포함)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.3 Redis 세션 설정 (RedisSessionConfig)
    - `sofit-user/global/config/`에 RedisSessionConfig.java 생성
    - `@EnableRedisIndexedHttpSession(maxInactiveIntervalInSeconds = 1800)` 사용
    - CookieSerializer Bean 등록 (HttpOnly, Secure, SameSite=Lax, cookieName="SESSION")
    - `sofit-user/build.gradle`에 spring-session-data-redis 의존성 추가
    - _Requirements: 9.1, 9.2, 9.5_

  - [x] 1.4 SessionValidationFilter 생성 (절대 만료 12시간)
    - `sofit-user/global/filter/`에 SessionValidationFilter.java 생성
    - 세션의 loginAt + 12시간 경과 시 세션 무효화 및 401 응답
    - 인증 불필요 경로(회원가입, 로그인)는 필터 스킵
    - _Requirements: 9.4, 9.6_

  - [x] 1.5 SecurityConfig 수정 (역할 기반 접근 제어)
    - BCryptPasswordEncoder Bean 등록
    - 역할 기반 접근 제어 설정: `/api/admin/loan-applications/**` → ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER
    - `/api/admin/products/**` → ADMIN_BANK_MANAGER
    - `/api/admin/users/**`, `/api/admin/logs/**` → ADMIN_DEV
    - `/api/admin/**` → USER 접근 거부
    - 인증 불필요 경로 설정: `/api/auth/signup/**`, `/api/auth/login`
    - SessionValidationFilter를 필터 체인에 등록
    - _Requirements: 6.2, 6.3, 6.4, 6.5, 6.6, 9.4_

  - [x] 1.6 AuthErrorCode / AuthSuccessCode 확장
    - AuthErrorCode에 AUTH4001~AUTH4011, AUTH5001 추가
    - AuthSuccessCode에 AUTH2001~AUTH2006 추가
    - UserErrorCode 신규 생성 (필요 시)
    - _Requirements: 전체 에러 처리_

  - [x] 1.7 ADMIN 시드 데이터 (data.sql)
    - `sofit-user/src/main/resources/data.sql` 생성
    - ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV 계정 INSERT (BCrypt 해싱된 비밀번호)
    - _Requirements: 6.1 (ADMIN은 시드 데이터로 관리)_

  - [ ]* 1.8 Phase 1 Property 테스트 (jqwik)
    - **Property 1: 입력 유효성 검증 정확성** — businessNumber, PIN, loginId, password, phoneNumber, residentNumber 정규식 검증
    - **Validates: Requirements 2.3, 3.6, 4.5, 4.6, 4.7, 4.8**

- [x] 2. Phase 1 Checkpoint
  - 컴파일 확인, 기존 테스트 통과 확인. 문제 발생 시 사용자에게 질문.

- [-] 3. Phase 2: 로그인 [SOFIT-20]
  - 세션 생성이 되어야 다른 인증 필요 API 테스트가 가능하므로 로그인을 먼저 구현한다.
  - 커밋: `[SOFIT-20] Feat: 세션 기반 로그인 API 구현`

  - [x] 3.1 LoginRequest / LoginResponse DTO 생성
    - `dto/request/LoginRequest.java` (class, @NotBlank loginId, @NotBlank password)
    - `dto/response/LoginResponse.java` (record: userId, name, role)
    - _Requirements: 5.1, 5.7_

  - [x] 3.2 AuthService 인터페이스에 login 메서드 추가 및 구현
    - `AuthService.java`에 `LoginResponse login(LoginRequest request, HttpSession session)` 추가
    - `AuthServiceImpl.java`에 로그인 로직 구현:
      - loginId로 User 조회 (미존재 시 AUTH4002)
      - status INACTIVE 체크 (AUTH4011)
      - BCrypt 비밀번호 검증 (불일치 시 AUTH4002 — 미존재와 동일 에러)
      - 세션에 userId, role, loginAt, PRINCIPAL_NAME_INDEX_NAME 저장
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

  - [ ] 3.3 AuthController에 로그인 엔드포인트 추가
    - `POST /api/auth/login` 엔드포인트 추가
    - AuthControllerDocs 인터페이스에 Swagger 문서 추가
    - 응답: `ApiResponse.onSuccess(AuthSuccessCode.AUTH2004, response)`
    - _Requirements: 5.1, 5.2_

  - [ ]* 3.4 로그인 단위 테스트
    - AuthServiceImpl.login() 성공/실패 시나리오 (Mockito)
    - **Property 7: 비밀번호 BCrypt 해싱 라운드트립**
    - **Property 8: 로그인 성공 시 세션 속성 저장**
    - **Property 9: 로그인 실패 응답 동일성**
    - **Validates: Requirements 4.11, 5.1, 5.2, 5.3, 5.4, 5.5**

- [ ] 4. Phase 2 Checkpoint
  - 컴파일 확인, 로그인 API 동작 확인. 문제 발생 시 사용자에게 질문.

- [ ] 5. Phase 3: 회원가입 Step 1 사업자등록번호 인증 수정 [SOFIT-21]
  - 기존 구현을 회원가입 멀티스텝 플로우에 맞게 수정한다.
  - 커밋: `[SOFIT-21] Feat: 회원가입 Step 1 사업자등록번호 인증 수정`

  - [ ] 5.1 VerifyBusinessRequest / VerifyBusinessResponse DTO 수정
    - 기존 BusinessVerificationRequest.java → VerifyBusinessRequest로 리네임 또는 수정 (class, @NotBlank @Pattern businessNumber)
    - 기존 BusinessVerificationResponse.java → VerifyBusinessResponse로 변경 (record: registrationId, businessNumber, representativeName, businessName, businessType, openDate)
    - _Requirements: 2.1, 2.3_

  - [ ] 5.2 AuthService에 verifyBusiness 메서드 수정
    - 기존 메서드 시그니처를 `VerifyBusinessResponse verifyBusiness(VerifyBusinessRequest request)`로 변경
    - 중복 가입 체크 로직 추가 (businessNumber로 ACTIVE User 조회 → AUTH4003)
    - ExternalMockClient 호출 후 RegistrationProcess 엔티티 생성 (registration_id UUID 발급, step=STEP_1_COMPLETED)
    - 응답에 registrationId 포함
    - _Requirements: 1.1, 2.1, 2.2, 2.4, 2.5, 2.6_

  - [ ] 5.3 AuthController의 사업자등록번호 인증 엔드포인트 수정
    - 기존 엔드포인트를 `POST /api/auth/signup/verify-business`로 변경
    - AuthControllerDocs 인터페이스 업데이트
    - 응답: `ApiResponse.onSuccess(AuthSuccessCode.AUTH2001, response)`
    - _Requirements: 2.1, 2.2_

  - [ ]* 5.4 Step 1 단위 테스트
    - verifyBusiness 성공/실패 시나리오 (중복 가입, KYC 실패, 타임아웃)
    - **Property 4: KYC 인증 성공 시 데이터 영속화**
    - **Property 14: 중복 사업자등록번호 가입 방지**
    - **Validates: Requirements 1.1, 2.2, 2.6**

- [ ] 6. Phase 3 Checkpoint
  - 컴파일 확인, Step 1 API 동작 확인. 문제 발생 시 사용자에게 질문.

- [ ] 7. Phase 4: 회원가입 Step 2 PIN 인증 수정 [SOFIT-22]
  - 기존 PIN 인증 구현을 회원가입 플로우에 맞게 수정한다.
  - 커밋: `[SOFIT-22] Feat: 회원가입 Step 2 PIN 인증 수정`

  - [ ] 7.1 VerifyPinRequest / VerifyPinResponse DTO 생성
    - VerifyPinRequest.java (class: @NotBlank registrationId, @NotBlank @Pattern pin 6자리, @NotBlank @Pattern phoneNumber 11자리)
    - VerifyPinResponse.java (record: registrationId, verified, verifiedAt)
    - 기존 FinancialCertVerifyRequest.java는 필요 시 수정 또는 대체
    - _Requirements: 3.1, 3.6_

  - [ ] 7.2 AuthService에 verifyPin 메서드 수정
    - `VerifyPinResponse verifyPin(VerifyPinRequest request)` 시그니처로 변경
    - registrationId로 RegistrationProcess 조회
    - 만료 체크 (created_at + 30분 → AUTH4008, step=EXPIRED)
    - Step 1 완료 여부 확인 (미완료 시 AUTH4007)
    - ExternalMockClient로 PIN 인증 요청
    - 금융인증서 상태 VALID 확인 + 실명 일치 검증 (불일치 시 AUTH4010)
    - 성공 시 RegistrationProcess 갱신 (pinVerified=true, pinVerifiedAt, step=STEP_2_COMPLETED)
    - _Requirements: 1.5, 3.1, 3.2, 3.3, 3.4, 3.5, 3.7, 3.8, 3.9_

  - [ ] 7.3 AuthController의 PIN 인증 엔드포인트 수정
    - 기존 엔드포인트를 `POST /api/auth/signup/verify-pin`으로 변경
    - AuthControllerDocs 인터페이스 업데이트
    - 응답: `ApiResponse.onSuccess(AuthSuccessCode.AUTH2002, response)`
    - _Requirements: 3.1, 3.3_

  - [ ]* 7.4 Step 2 단위 테스트
    - verifyPin 성공/실패 시나리오 (만료, 단계 미완료, PIN 불일치, 인증서 상태 불일치, 실명 불일치)
    - **Property 2: 회원가입 임시 토큰 만료**
    - **Property 3: 회원가입 단계 순서 보장**
    - **Property 5: PIN 인증 및 금융인증서 검증**
    - **Validates: Requirements 1.5, 3.1, 3.2, 3.3, 3.5, 3.8, 4.1, 4.9**

- [ ] 8. Phase 4 Checkpoint
  - 컴파일 확인, Step 2 API 동작 확인. 문제 발생 시 사용자에게 질문.

- [ ] 9. Phase 5: 회원가입 Step 3 가입 완료 [SOFIT-23]
  - 신규 구현. Step 2 완료 후 고객정보를 입력받아 회원가입을 완료한다.
  - 커밋: `[SOFIT-23] Feat: 회원가입 Step 3 가입 완료 API 구현`

  - [ ] 9.1 SignupCompleteRequest / SignupCompleteResponse DTO 생성
    - SignupCompleteRequest.java (class: @NotBlank registrationId, @NotBlank @Pattern loginId, @NotBlank @Pattern password, @NotBlank name, @NotBlank @Pattern residentNumber 7자리, @NotBlank @Pattern phoneNumber 11자리)
    - SignupCompleteResponse.java (record: userId, loginId, name, role)
    - _Requirements: 4.1, 4.5, 4.6, 4.7, 4.8_

  - [ ] 9.2 AuthService에 completeSignup 메서드 추가 및 구현
    - `SignupCompleteResponse completeSignup(SignupCompleteRequest request)` 추가
    - `@Transactional`로 User 생성 + BusinessProfile 생성 + RegistrationProcess 갱신을 하나의 트랜잭션으로 묶어 처리
    - registrationId로 RegistrationProcess 조회 + 만료 체크 + Step 2 완료 확인
    - loginId 중복 체크 (AUTH4009)
    - BCrypt 비밀번호 해싱
    - User.createUser(...) 호출하여 User 생성 (role=USER, status=ACTIVE)
    - RegistrationProcess 갱신 (user_id 연결, registration_id=null, step=COMPLETED)
    - KYC 데이터를 기반으로 BusinessProfile 생성하여 User에 연결
    - _Requirements: 1.4, 4.1, 4.2, 4.3, 4.4, 4.9, 4.10, 4.11_

  - [ ] 9.3 AuthController에 가입 완료 엔드포인트 추가
    - `POST /api/auth/signup/complete` 엔드포인트 추가
    - AuthControllerDocs 인터페이스 업데이트
    - 응답: `ApiResponse.onSuccess(AuthSuccessCode.AUTH2003, response)`
    - _Requirements: 4.1, 4.2_

  - [ ]* 9.4 Step 3 단위 테스트
    - completeSignup 성공/실패 시나리오 (만료, 단계 미완료, 아이디 중복)
    - **Property 6: 회원가입 완료 상태 전이**
    - **Validates: Requirements 1.4, 4.2, 4.3, 4.4**

- [ ] 10. Phase 5 Checkpoint
  - 컴파일 확인, Step 3 API 동작 확인, 전체 회원가입 플로우(Step 1→2→3) 연결 확인. 문제 발생 시 사용자에게 질문.

- [ ] 11. Phase 6: 로그아웃 [SOFIT-23]
  - 세션 삭제를 통한 로그아웃 기능을 구현한다.
  - 커밋: `[SOFIT-23] Feat: 로그아웃 API 구현`

  - [ ] 11.1 AuthService에 logout 메서드 추가 및 구현
    - `void logout(HttpSession session)` 추가
    - session.invalidate() 호출하여 세션 삭제
    - _Requirements: 7.1, 7.2_

  - [ ] 11.2 AuthController에 로그아웃 엔드포인트 추가
    - `POST /api/auth/logout` 엔드포인트 추가 (인증 필요)
    - AuthControllerDocs 인터페이스 업데이트
    - 응답: `ApiResponse.onSuccess(AuthSuccessCode.AUTH2005, null)`
    - _Requirements: 7.1, 7.4, 7.5_

  - [ ]* 11.3 로그아웃 단위 테스트
    - logout 성공 시나리오
    - **Property 11: 로그아웃 세션 무효화**
    - **Validates: Requirements 7.1, 7.5**

- [ ] 12. Phase 6 Checkpoint
  - 컴파일 확인, 로그아웃 API 동작 확인. 문제 발생 시 사용자에게 질문.

- [ ] 13. Phase 7: 회원탈퇴 [SOFIT-27]
  - Soft Delete 방식의 회원탈퇴를 구현한다. 모든 활성 세션 삭제를 포함한다.
  - 커밋: `[SOFIT-27] Feat: 회원탈퇴 API 구현 (Soft Delete + 전체 세션 삭제)`

  - [ ] 13.1 UserService / UserServiceImpl 생성
    - `sofit-user/domain/user/service/`에 UserService.java (interface) 생성
    - UserServiceImpl.java 구현: `void withdraw(Long userId, HttpSession session)`
    - FindByIndexNameSessionRepository 주입하여 userId로 모든 활성 세션 조회 및 삭제
    - User.inactivate() 호출 (status=INACTIVE, inactivatedAt 기록)
    - _Requirements: 8.1, 8.2, 8.3_

  - [ ] 13.2 UserController / UserControllerDocs 생성
    - `sofit-user/domain/user/controller/`에 UserController.java 생성
    - `DELETE /api/users/me` 엔드포인트 (인증 필요, USER 역할만)
    - UserControllerDocs.java 인터페이스 생성 (Swagger 문서)
    - 세션에서 userId 추출하여 UserService.withdraw() 호출
    - 응답: `ApiResponse.onSuccess(AuthSuccessCode.AUTH2006, null)`
    - _Requirements: 8.1, 8.4, 8.5_

  - [ ]* 13.3 회원탈퇴 단위 테스트
    - withdraw 성공 시나리오 (상태 변경 + 세션 삭제 확인)
    - **Property 12: 회원탈퇴 상태 전이**
    - **Validates: Requirements 8.1, 8.2, 8.3**

- [ ] 14. Phase 7 Checkpoint — 최종 확인
  - 전체 컴파일 확인, 모든 API 동작 확인. 문제 발생 시 사용자에게 질문.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 Phase는 1 커밋 단위로 관리. Phase 완료 후 사용자가 직접 커밋한다.
- git 명령어는 사용자에게 안내만 제공 (직접 실행하지 않음)
- Property 테스트는 jqwik 라이브러리를 사용하며, 선택적으로 구현한다.
- DTO 컨벤션: Request → class (Bean Validation), Response → record
- 기존 코드 수정 시 기존 패턴과 일관성을 유지한다.
- ADMIN 계정은 data.sql로 시드 데이터 관리 (회원가입 플로우 없음)
- 세션 역조회: `@EnableRedisIndexedHttpSession` + `FindByIndexNameSessionRepository` 사용
- 만료 체크: 스케줄러 없이 요청 시점 lazy check (created_at + 30분)
- 절대 세션 만료: SessionValidationFilter에서 loginAt + 12시간 체크

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.6"] },
    { "id": 1, "tasks": ["1.3", "1.4", "1.7"] },
    { "id": 2, "tasks": ["1.5", "1.8"] },
    { "id": 3, "tasks": ["3.1"] },
    { "id": 4, "tasks": ["3.2"] },
    { "id": 5, "tasks": ["3.3", "3.4"] },
    { "id": 6, "tasks": ["5.1"] },
    { "id": 7, "tasks": ["5.2"] },
    { "id": 8, "tasks": ["5.3", "5.4"] },
    { "id": 9, "tasks": ["7.1"] },
    { "id": 10, "tasks": ["7.2"] },
    { "id": 11, "tasks": ["7.3", "7.4"] },
    { "id": 12, "tasks": ["9.1"] },
    { "id": 13, "tasks": ["9.2"] },
    { "id": 14, "tasks": ["9.3", "9.4"] },
    { "id": 15, "tasks": ["11.1"] },
    { "id": 16, "tasks": ["11.2", "11.3"] },
    { "id": 17, "tasks": ["13.1"] },
    { "id": 18, "tasks": ["13.2", "13.3"] }
  ]
}
```
