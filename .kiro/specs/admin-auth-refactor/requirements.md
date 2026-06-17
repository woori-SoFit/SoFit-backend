# Requirements Document

## Introduction

Admin 인증 방식 리팩토링 (SOFIT-123). 현재 sofit-admin 모듈의 로그인 및 내 정보 조회 구현에서 userId 이중 저장, SecurityContext 미활용 문제를 해결하고, 세션 보안을 강화한다. 세션 속성 직접 저장을 제거하고 Spring Security의 SecurityContextHolder를 단일 인증 정보 소스로 통일하며, 세션 고정 공격 방지·동시 로그인 제한·Secure 쿠키·역할 기반 접근 제어 세분화를 적용한다.

## Glossary

- **Admin_Auth_Service**: sofit-admin 모듈의 관리자 인증 서비스 (로그인, 내 정보 조회)
- **SecurityContext**: Spring Security가 관리하는 인증 정보 저장소 (SecurityContextHolder를 통해 접근)
- **Session**: Redis에 저장되는 HTTP 세션
- **SecurityConfig**: sofit-admin 모듈의 Spring Security 설정 클래스
- **Admin_Auth_Controller**: 관리자 인증 API 컨트롤러
- **ADMIN_BANK_TELLER**: 은행 창구 직원 역할
- **ADMIN_BANK_MANAGER**: 은행 지점장 역할
- **ADMIN_DEV**: 개발자 관리자 역할

## Requirements

### Requirement 1: 로그인 시 세션 속성 직접 저장 제거

**User Story:** As a 개발자, I want 로그인 시 userId/role/loginTime 세션 속성 직접 저장을 제거하고 SecurityContext만 사용하도록 통일하고 싶다, so that 인증 정보의 단일 소스(Single Source of Truth)를 유지하고 이중 저장으로 인한 불일치 위험을 제거할 수 있다.

#### Acceptance Criteria

1. WHEN 관리자가 로그인에 성공하면, THE Admin_Auth_Service SHALL session.setAttribute("userId", ...), session.setAttribute("role", ...), session.setAttribute("loginTime", ...) 호출을 수행하지 않으며, 해당 키로 세션에 값이 존재하지 않아야 한다
2. WHEN 관리자가 로그인에 성공하면, THE Admin_Auth_Service SHALL UsernamePasswordAuthenticationToken을 생성하여 SecurityContext에 설정하고, HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY로 세션에 저장한다
3. WHEN 관리자가 로그인에 성공하면, THE Admin_Auth_Service SHALL Authentication의 principal에 userId(Long 타입)를, authorities에 UserRole의 name() 값을 SimpleGrantedAuthority로 감싸서 저장한다
4. WHEN 인증된 관리자의 userId가 필요한 경우, THE Admin_Auth_Service SHALL SecurityContextHolder.getContext().getAuthentication().getPrincipal()에서 userId(Long)를 조회하며, session.getAttribute("userId")를 사용하지 않는다
5. WHEN 인증된 관리자의 역할(role)이 필요한 경우, THE Admin_Auth_Service SHALL SecurityContextHolder.getContext().getAuthentication().getAuthorities()에서 역할을 조회하며, session.getAttribute("role")를 사용하지 않는다

### Requirement 2: findMe() HttpSession 파라미터 제거 및 SecurityContext 활용

**User Story:** As a 개발자, I want findMe() 메서드에서 HttpSession 파라미터를 제거하고 SecurityContextHolder에서 userId를 조회하고 싶다, so that Spring Security의 인증 메커니즘을 일관되게 활용하고 컨트롤러-서비스 간 불필요한 의존을 제거할 수 있다.

#### Acceptance Criteria

1. THE Admin_Auth_Service SHALL findMe() 메서드의 시그니처에서 HttpSession 파라미터를 제거하고, AdminAuthService 인터페이스와 AdminAuthServiceImpl 구현체 모두에 적용한다
2. WHEN findMe()가 호출되면, THE Admin_Auth_Service SHALL SecurityContextHolder.getContext().getAuthentication().getPrincipal()에서 userId(Long 타입)를 추출하여 사용자 조회에 사용한다
3. THE Admin_Auth_Controller SHALL findMe() 엔드포인트 메서드 시그니처에서 HttpSession 파라미터를 제거하고, AdminAuthService.findMe()를 파라미터 없이 호출한다
4. IF SecurityContext에 Authentication이 존재하지 않거나 principal이 null이면, THEN THE Admin_Auth_Service SHALL AdminAuthErrorCode에 정의된 인증 관련 에러코드와 함께 BaseException을 발생시켜 HTTP 401 응답을 반환한다
5. IF principal이 Long 타입으로 변환할 수 없는 값이면, THEN THE Admin_Auth_Service SHALL 인증 실패로 간주하여 BaseException을 발생시켜 HTTP 401 응답을 반환한다
6. WHEN findMe()에서 userId로 사용자를 조회한 결과 사용자의 status가 INACTIVE이면, THE Admin_Auth_Service SHALL AdminAuthErrorCode.USER_NOT_FOUND 에러코드와 함께 BaseException을 발생시킨다

### Requirement 3: 추후 Admin API의 인증 정보 접근 방식 통일

**User Story:** As a 개발자, I want 모든 Admin API에서 HttpSession을 직접 받지 않고 SecurityContextHolder에서 userId를 꺼내 사용하는 패턴을 적용하고 싶다, so that 인증 정보 접근 방식이 프로젝트 전체에서 일관되게 유지된다.

#### Acceptance Criteria

1. THE Admin_Auth_Service SHALL login() 메서드를 제외한 모든 Service 및 Controller 메서드의 파라미터에서 HttpSession을 사용하지 않는다
2. IF 인증된 사용자의 userId가 필요한 경우, THEN THE Admin_Auth_Service SHALL SecurityContextHolder.getContext().getAuthentication().getPrincipal()을 통해 userId를 조회한다
3. IF SecurityContextHolder에서 Authentication이 존재하지 않거나 Principal이 null인 경우, THEN THE Admin_Auth_Service SHALL 인증 실패를 나타내는 예외를 발생시킨다

### Requirement 4: 세션 고정 공격 방지

**User Story:** As a 보안 담당자, I want 로그인 성공 시 새 세션 ID를 발급하고 싶다, so that 세션 고정 공격(Session Fixation Attack)을 방지할 수 있다.

#### Acceptance Criteria

1. WHEN 관리자가 로그인에 성공하면, THE SecurityConfig SHALL 로그인 이전의 세션 ID와 다른 새로운 세션 ID를 발급한다 (sessionFixation().newSession())
2. WHEN 새 세션이 생성되면, THE SecurityConfig SHALL 기존 세션의 속성(userId, role, loginTime, SecurityContext)을 새 세션으로 마이그레이션하지 않는다
3. WHEN 로그인 성공으로 새 세션이 발급되면, THE SecurityConfig SHALL 로그인 이전에 존재하던 기존 세션 ID를 무효화하여 해당 세션 ID로의 접근을 거부한다
4. WHEN 새 세션이 발급된 후, THE SecurityConfig SHALL 새 세션에 인증 정보(SecurityContext, userId, role)를 저장하여 이후 요청에서 인증 상태를 유지한다

### Requirement 5: 동시 로그인 방지

**User Story:** As a 보안 담당자, I want 동일 관리자 계정의 동시 세션을 1개로 제한하고 싶다, so that 계정 공유나 탈취된 세션의 동시 사용을 방지할 수 있다.

#### Acceptance Criteria

1. WHILE 관리자 계정(BANK_ADMIN, DEV_ADMIN)으로 활성 세션이 1개 존재하면, THE 시스템 SHALL 동일 계정의 추가 로그인 시도를 차단하고 동시 로그인 제한을 나타내는 에러 응답을 반환한다
2. WHEN 동일 계정으로 추가 로그인이 시도되어 차단되면, THE 시스템 SHALL 기존 세션 정보를 유지하고 새 세션을 생성하지 않는다
3. WHEN 기존 세션이 만료되거나 로그아웃으로 종료되면, THE 시스템 SHALL 해당 계정의 신규 로그인 시도를 허용한다
4. IF 세션 저장소 조회에 실패하면, THEN THE 시스템 SHALL 로그인 요청을 거부하고 서버 오류를 나타내는 에러 응답을 반환한다

### Requirement 6: Secure 쿠키 설정

**User Story:** As a 보안 담당자, I want 운영 환경에서 세션 쿠키에 Secure/HttpOnly/SameSite 속성을 적용하고 싶다, so that 쿠키 탈취 및 CSRF 공격 위험을 줄일 수 있다.

#### Acceptance Criteria

1. WHILE 운영 프로파일(release)이 활성화되어 있으면, THE SecurityConfig SHALL Spring Session이 관리하는 세션 쿠키에 secure=true 속성을 적용한다
2. WHILE 운영 프로파일(release)이 활성화되어 있으면, THE SecurityConfig SHALL Spring Session이 관리하는 세션 쿠키에 http-only=true 속성을 적용한다
3. WHILE 운영 프로파일(release)이 활성화되어 있으면, THE SecurityConfig SHALL Spring Session이 관리하는 세션 쿠키에 same-site=strict 속성을 적용한다
4. WHILE 비운영 프로파일(local 또는 dev)이 활성화되어 있으면, THE SecurityConfig SHALL 세션 쿠키에 secure=false를 적용하고, http-only=true 및 same-site=lax 속성을 적용한다

### Requirement 7: 역할 기반 접근 제어 세분화

**User Story:** As a 보안 담당자, I want /api/admin/** 전체에 동일 권한을 부여하는 대신 행위별로 역할 기반 접근 제어를 세분화하고 싶다, so that 최소 권한 원칙(Principle of Least Privilege)을 적용하여 권한 오남용을 방지할 수 있다.

#### Acceptance Criteria

1. THE SecurityConfig SHALL /api/admin/** 경로에 대해 ADMIN_DEV, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER 모든 관리자 역할의 접근을 기본적으로 허용한다
2. WHEN 지점장 결재 조회 요청(/api/admin/manager/loan-applications/{applicationId}/approve)이 수신되면, THE SecurityConfig SHALL ADMIN_BANK_MANAGER 권한을 가진 관리자만 접근을 허용하고, ADMIN_BANK_TELLER 및 ADMIN_DEV 권한의 접근을 차단한다
3. WHEN S등급 배치 관리 조회 요청(/api/admin/dev/batch/s-grade)이 수신되면, THE SecurityConfig SHALL ADMIN_DEV 권한을 가진 관리자만 접근을 허용하고, ADMIN_BANK_TELLER 및 ADMIN_BANK_MANAGER 권한의 접근을 차단한다
4. WHEN API 로그 조회 요청(/api/admin/dev/logs/api)이 수신되면, THE SecurityConfig SHALL ADMIN_DEV 권한을 가진 관리자만 접근을 허용하고, ADMIN_BANK_TELLER 및 ADMIN_BANK_MANAGER 권한의 접근을 차단한다
5. IF 권한이 부족한 관리자가 자신의 역할에 허용되지 않은 엔드포인트에 접근하면, THEN THE SecurityConfig SHALL HTTP 403 상태 코드와 함께 공통 응답 포맷(isSuccess: false, code: "COMMON4003", message: "권한이 없습니다.")의 JSON 응답을 반환한다
6. THE SecurityConfig SHALL 세분화된 예외 규칙(AC 2, 3, 4)을 /api/admin/** 전체 허용 규칙보다 먼저 평가하여, 구체적인 경로 규칙이 우선 적용되도록 한다
