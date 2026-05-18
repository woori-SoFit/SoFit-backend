# Requirements Document

## Introduction

SoFit 대출 플랫폼의 인증 및 사용자 관리 기능을 정의한다. 멀티스텝 회원가입(사업자등록번호 인증 → 금융인증서 PIN 인증 → 고객정보 입력), 세션 기반 로그인, 로그아웃, 회원탈퇴(soft delete) API를 구현하며, Redis를 외부 세션 저장소로 사용한다. 회원가입 중간 인증 결과는 개인정보 보호를 위해 별도의 RegistrationProcess 테이블에 임시 저장하며, 가입 완료 시 BusinessProfile을 생성한다.

> **참고**: 사업자등록번호 인증(Step 1)과 금융인증서 PIN 인증(Step 2)은 기존에 구현되어 있으며, 회원가입 멀티스텝 플로우에 맞게 수정한다.

## Glossary

- **Auth_System**: 인증 및 사용자 관리를 담당하는 시스템 (sofit-user 모듈의 auth 도메인)
- **Session_Store**: Redis 기반 외부 세션 저장소
- **RegistrationProcess**: 회원가입 멀티스텝 진행 중 인증 결과를 임시 저장하는 별도 테이블. registration_id(UUID)로 프로세스를 식별하며, 가입 완료 시 user_id가 연결되고 registration_id는 null로 설정된다.
- **BusinessProfile**: 가입 완료 시 KYC 인증 데이터를 기반으로 생성되는 사업자 프로필 엔티티.
- **External_Mock_Server**: 국세청 API, 금융인증서 API를 모사하는 외부 Mock 서버
- **KYC_Verification**: 사업자등록번호 진위 확인을 통한 본인 인증 절차
- **Financial_Cert_PIN**: 금융인증서 PIN(6자리) 인증 절차
- **User**: 소상공인 고객 역할
- **Bank_Admin**: 은행원 역할 (ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER로 세분화)
- **Dev_Admin**: 개발자 역할 (ADMIN_DEV)
- **Soft_Delete**: 물리 삭제 없이 상태값을 INACTIVE로 변경하는 논리 삭제 방식

## Requirements

### Requirement 1: 멀티스텝 회원가입 - 임시 저장소 관리 [SOFIT-23]

**User Story:** As a 소상공인 고객, I want 회원가입 중간 인증 결과가 안전하게 저장되기를, so that 인증 단계를 순차적으로 진행할 수 있다.

#### Acceptance Criteria

1. WHEN 회원가입 Step 1 사업자등록번호 인증 요청이 수신되면, THE Auth_System SHALL UUID v4 형식의 registration_id를 생성하여 RegistrationProcess 테이블에 레코드를 생성하고, step을 STEP_1_COMPLETED로 설정한다.
2. THE RegistrationProcess 테이블 SHALL registration_id(UUID, 회원가입 프로세스 임시 식별자, 가입 완료 후 null), user_id(nullable, 가입 완료 후 채워짐), 인증 단계 상태(step: STEP_1_COMPLETED, STEP_2_COMPLETED, COMPLETED, EXPIRED 중 하나), KYC 인증 결과(사업자등록번호, 상호명, 대표자명, 개업일, 업종), PIN 인증 결과(인증 성공 여부, 인증 시각), 생성 시각을 저장한다.
3. WHILE 회원가입이 완료되지 않은 상태(step이 COMPLETED 또는 EXPIRED가 아닌 상태)에서, THE Auth_System SHALL registration_id를 통해 이전 단계 인증 결과를 조회한다.
4. WHEN 회원가입이 완료되면(Step 3 고객정보 저장 성공 시), THE Auth_System SHALL 해당 RegistrationProcess 레코드의 registration_id를 null로 설정하고 user_id를 연결하며, KYC 데이터를 기반으로 별도의 BusinessProfile을 생성한다.
5. WHEN registration_id로 단계 진행 요청이 수신되면, THE Auth_System SHALL created_at 기준으로 30분 경과 여부를 확인하고, 경과한 경우 step을 EXPIRED로 변경 후 토큰 만료 에러를 반환한다. (스케줄러 방식이 아닌 요청 시점 체크 방식으로 처리)

### Requirement 2: 멀티스텝 회원가입 - Step 1 사업자등록번호 인증 [SOFIT-21]

> **참고**: 기존 구현을 회원가입 플로우에 맞게 수정

**User Story:** As a 소상공인 고객, I want 사업자등록번호를 인증하여, so that 사업자 본인임을 확인받을 수 있다.

#### Acceptance Criteria

1. WHEN 사업자등록번호(하이픈 없는 10자리)가 `POST /api/auth/signup/verify-business` 요청으로 전달되면, THE Auth_System SHALL External_Mock_Server에 사업자등록번호 진위 확인을 요청하고 응답을 5초 이내에 수신한다.
2. WHEN External_Mock_Server가 인증 성공 응답을 반환하면, THE Auth_System SHALL KYC 인증 결과(상호명, 업종, 개업일, 대표자명)를 RegistrationProcess 테이블에 저장하고 registration_id를 응답한다.
3. IF 사업자등록번호가 10자리 숫자가 아닌 경우, THEN THE Auth_System SHALL 유효성 검증 실패 에러를 반환한다.
4. IF External_Mock_Server가 인증 실패 응답을 반환하면, THEN THE Auth_System SHALL 사업자등록번호 인증 실패 에러를 반환한다.
5. IF External_Mock_Server로부터 5초 이내에 응답을 수신하지 못한 경우, THEN THE Auth_System SHALL 외부 서비스 연결 실패를 나타내는 에러를 반환한다.
6. IF 해당 사업자등록번호로 이미 ACTIVE 상태의 User가 존재하는 경우, THEN THE Auth_System SHALL 이미 가입된 사업자등록번호임을 나타내는 에러를 반환한다.

### Requirement 3: 멀티스텝 회원가입 - Step 2 금융인증서 PIN 인증 [SOFIT-22]

> **참고**: 기존 구현을 회원가입 플로우에 맞게 수정

**User Story:** As a 소상공인 고객, I want 금융인증서 PIN을 인증하여, so that 금융 본인 확인을 완료할 수 있다.

#### Acceptance Criteria

1. WHEN registration_id와 PIN(6자리 숫자)이 `POST /api/auth/signup/verify-pin` 요청으로 전달되면, THE Auth_System SHALL RegistrationProcess 테이블에서 해당 registration_id의 Step 1 완료 여부를 확인한다.
2. WHEN Step 1이 완료된 상태에서 PIN이 전달되면, THE Auth_System SHALL External_Mock_Server에 금융인증서 PIN 인증을 요청하고, 해당 금융인증서의 상태가 VALID인지와 인증서의 실명이 Step 1에서 확인된 대표자명과 일치하는지를 검증한다.
3. WHEN External_Mock_Server가 PIN 인증 성공 응답을 반환하고 금융인증서 상태가 VALID이며 실명이 일치하면, THE Auth_System SHALL 인증 성공 여부와 인증 시각을 RegistrationProcess 테이블에 저장하고 step을 STEP_2_COMPLETED로 갱신한다.
4. IF registration_id가 유효하지 않거나 만료된 경우, THEN THE Auth_System SHALL 만료 에러를 반환한다.
5. IF Step 1이 완료되지 않은 상태에서 Step 2 요청이 들어오면, THEN THE Auth_System SHALL 이전 단계 미완료 에러를 반환한다.
6. IF PIN이 6자리 숫자가 아닌 경우, THEN THE Auth_System SHALL 유효성 검증 실패 에러를 반환한다.
7. IF External_Mock_Server가 PIN 인증 실패 응답을 반환하면, THEN THE Auth_System SHALL PIN 인증 실패 에러를 반환한다.
8. IF 금융인증서 상태가 VALID가 아니거나 실명이 일치하지 않는 경우, THEN THE Auth_System SHALL 금융인증서 검증 실패 에러를 반환한다.
9. IF External_Mock_Server와의 통신이 5초 이내에 응답하지 않거나 연결에 실패하면, THEN THE Auth_System SHALL 외부 서비스 연결 실패 에러를 반환한다.

### Requirement 4: 멀티스텝 회원가입 - Step 3 고객정보 입력 및 가입 완료 [SOFIT-23]

**User Story:** As a 소상공인 고객, I want 개인정보와 아이디/비밀번호를 입력하여, so that 회원가입을 완료할 수 있다.

> **참고**: 프론트엔드에서는 이름/주민번호/휴대폰 입력 → PIN 인증 → 아이디/비밀번호 입력 순서로 화면이 분리되지만, 백엔드 API는 Step 2 완료 후 마지막에 한 번 요청으로 처리한다.

#### Acceptance Criteria

1. WHEN registration_id와 고객정보(아이디, 비밀번호, 이름, 주민번호 앞 7자리, 연락처)가 `POST /api/auth/signup/complete` 요청으로 전달되면, THE Auth_System SHALL RegistrationProcess 테이블에서 Step 1, Step 2 완료 여부를 확인한다.
2. WHEN 모든 이전 단계가 완료된 상태에서 고객정보가 전달되면, THE Auth_System SHALL User 엔티티를 생성하고, RegistrationProcess의 user_id를 연결하며, KYC 데이터를 기반으로 BusinessProfile을 생성하여 User에 연결한다.
3. WHEN 회원가입이 완료되면, THE Auth_System SHALL User의 role을 USER로 설정하고 status를 ACTIVE로 설정한다.
4. IF 아이디가 이미 존재하는 경우, THEN THE Auth_System SHALL 아이디 중복 에러를 반환한다.
5. IF 아이디가 4자 이상 20자 이하의 영문 소문자 및 숫자 조합이 아닌 경우, THEN THE Auth_System SHALL 아이디 형식 유효성 검증 실패 에러를 반환한다.
6. IF 비밀번호가 8자 이상 20자 이하이며 영문, 숫자, 특수문자를 각각 1자 이상 포함하는 조합이 아닌 경우, THEN THE Auth_System SHALL 비밀번호 형식 유효성 검증 실패 에러를 반환한다.
7. IF 연락처가 하이픈 없는 11자리 숫자가 아닌 경우, THEN THE Auth_System SHALL 연락처 유효성 검증 실패 에러를 반환한다.
8. IF 주민번호 앞 7자리가 숫자 7자리(생년월일 6자리 + 성별코드 1자리) 형식이 아닌 경우, THEN THE Auth_System SHALL 주민번호 유효성 검증 실패 에러를 반환한다.
9. IF Step 1 또는 Step 2가 완료되지 않은 상태에서 Step 3 요청이 들어오면, THEN THE Auth_System SHALL 이전 단계 미완료 에러를 반환한다.
10. IF registration_id가 유효하지 않거나 만료된 경우, THEN THE Auth_System SHALL 만료 에러를 반환한다.
11. WHEN 회원가입이 완료되면, THE Auth_System SHALL 비밀번호를 BCrypt로 해싱하여 저장한다.

### Requirement 5: 세션 기반 로그인 [SOFIT-20]

**User Story:** As a 사용자(USER, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV), I want 아이디와 비밀번호로 로그인하여, so that 인증된 상태로 서비스를 이용할 수 있다.

#### Acceptance Criteria

1. WHEN 아이디와 비밀번호가 `POST /api/auth/login` 요청으로 전달되면, THE Auth_System SHALL 아이디로 사용자를 조회하고 저장된 BCrypt 해시와 비밀번호를 검증한다.
2. WHEN 인증이 성공하면, THE Auth_System SHALL Session_Store에 세션을 생성하고 세션 ID를 쿠키로 반환하며, 응답 본문에 사용자 ID, 역할(role), 이름을 포함한다.
3. WHEN 인증이 성공하면, THE Auth_System SHALL 세션에 사용자 ID, 역할(role), 로그인 시각을 저장한다.
4. IF 아이디에 해당하는 사용자가 존재하지 않는 경우, THEN THE Auth_System SHALL 아이디 또는 비밀번호 불일치와 동일한 인증 실패 에러를 반환한다.
5. IF 비밀번호가 일치하지 않는 경우, THEN THE Auth_System SHALL 사용자 미존재와 동일한 인증 실패 에러를 반환한다.
6. IF 사용자의 status가 INACTIVE인 경우, THEN THE Auth_System SHALL 탈퇴한 계정임을 나타내는 에러를 반환한다.
7. IF 아이디 또는 비밀번호가 빈 값이거나 누락된 경우, THEN THE Auth_System SHALL 유효성 검증 실패 에러를 반환한다.

### Requirement 6: 역할 세분화 [SOFIT-20]

**User Story:** As a 시스템 관리자, I want 은행원 역할을 직급별로 세분화하여, so that 직급에 따른 권한 관리를 할 수 있다.

#### Acceptance Criteria

1. THE Auth_System SHALL 다음 역할을 enum으로 정의한다: USER(고객), ADMIN_BANK_TELLER(은행원), ADMIN_BANK_MANAGER(지점장), ADMIN_DEV(개발자).
2. WHILE 사용자가 ADMIN_BANK_TELLER 역할을 가진 상태에서, THE Auth_System SHALL 대출 심사 처리 관련 엔드포인트(`/api/admin/loan-applications/**`)에 대한 접근을 허용한다.
3. WHILE 사용자가 ADMIN_BANK_MANAGER 역할을 가진 상태에서, THE Auth_System SHALL 대출 심사 처리 관련 엔드포인트(`/api/admin/loan-applications/**`) 및 상품 관리 관련 엔드포인트(`/api/admin/products/**`)에 대한 접근을 허용한다.
4. WHILE 사용자가 ADMIN_DEV 역할을 가진 상태에서, THE Auth_System SHALL 사용자 관리 관련 엔드포인트(`/api/admin/users/**`) 및 API 로그 조회 관련 엔드포인트(`/api/admin/logs/**`)에 대한 접근을 허용한다.
5. IF 인증된 사용자가 자신의 역할에 허용되지 않은 엔드포인트에 접근하면, THEN THE Auth_System SHALL 권한 없음 에러(403)를 반환하고 요청을 거부한다.
6. WHILE 사용자가 USER 역할을 가진 상태에서, THE Auth_System SHALL `/api/admin/**` 하위 모든 엔드포인트에 대한 접근을 거부한다.

### Requirement 7: 로그아웃 [SOFIT-23]

**User Story:** As a 인증된 사용자, I want 로그아웃하여, so that 세션을 안전하게 종료할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 사용자가 `POST /api/auth/logout` 요청을 보내면, THE Auth_System SHALL Session_Store에서 해당 세션을 삭제하고 성공 응답을 반환한다.
2. WHEN 로그아웃이 완료되면, THE Auth_System SHALL 세션 쿠키의 Max-Age를 0으로 설정하여 클라이언트에서 쿠키가 즉시 삭제되도록 한다.
3. WHEN 세션 유휴 만료 시간(30분) 또는 절대 만료 시간(12시간)이 경과하면, THE Session_Store SHALL 해당 세션을 자동으로 만료 처리한다.
4. IF 유효하지 않은 세션으로 로그아웃 요청이 들어오면, THEN THE Auth_System SHALL 인증 필요 에러(COMMON4001)를 반환한다.
5. WHEN 로그아웃이 완료된 후 삭제된 세션 ID로 API 요청이 들어오면, THE Auth_System SHALL 인증 필요 에러(COMMON4001)를 반환한다.

### Requirement 8: 회원탈퇴 (Soft Delete) [SOFIT-27]

**User Story:** As a 소상공인 고객, I want 회원탈퇴를 요청하여, so that 서비스 이용을 중단할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 USER가 `DELETE /api/users/me` 요청을 보내면, THE Auth_System SHALL 해당 User의 status를 INACTIVE로 변경한다.
2. WHEN 회원탈퇴가 완료되면, THE Auth_System SHALL Session_Store에서 해당 사용자의 모든 활성 세션을 삭제한다.
3. WHEN 회원탈퇴가 완료되면, THE Auth_System SHALL 탈퇴 일시를 User 엔티티에 기록한다.
4. IF 인증되지 않은 요청이 들어오면, THEN THE Auth_System SHALL 인증 필요 에러를 반환한다.
5. IF USER 역할이 아닌 사용자가 `DELETE /api/users/me` 요청을 보내면, THEN THE Auth_System SHALL 권한 없음 에러를 반환한다.
6. THE Auth_System SHALL 탈퇴 후 개인정보 보관 기간을 30일로 설정한다. **(TODO: 현재 스프린트 구현 범위 외)**
7. WHEN 개인정보 보관 기간(30일)이 경과하면, THE Auth_System SHALL 해당 사용자의 개인정보(이름, 주민번호, 연락처)를 복구 불가능하게 익명화 처리하고, 대출 이력 및 감사용 데이터는 보존한다. **(TODO: 현재 스프린트 구현 범위 외)**

### Requirement 9: 세션 관리 [SOFIT-20]

**User Story:** As a 시스템 운영자, I want 세션이 안전하게 관리되기를, so that 인증 상태가 일관되게 유지된다.

#### Acceptance Criteria

1. THE Session_Store SHALL Redis를 외부 세션 저장소로 사용한다.
2. THE Auth_System SHALL 세션 유휴 만료 시간(idle timeout)을 30분으로 설정한다.
3. WHEN 인증된 사용자가 API 요청을 보내면, THE Auth_System SHALL 세션 유휴 만료 시간을 마지막 요청 시점으로부터 30분으로 재설정한다(슬라이딩 세션).
4. THE Auth_System SHALL 세션 절대 만료 시간(absolute timeout)을 12시간으로 설정하고, 로그인 시점으로부터 12시간이 경과하면 활동 여부와 관계없이 세션을 강제 만료한다.
5. THE Auth_System SHALL 세션 ID를 HttpOnly, Secure, SameSite=Lax 속성의 쿠키로 관리한다.
6. IF 유휴 만료(30분 무활동) 또는 절대 만료(12시간 경과)된 세션으로 API 요청이 들어오면, THEN THE Auth_System SHALL 세션 쿠키를 무효화하고 인증 필요 에러(401)를 반환한다.
7. IF 세션 쿠키가 없는 상태에서 인증이 필요한 API 요청이 들어오면, THEN THE Auth_System SHALL 인증 필요 에러(401)를 반환한다.
