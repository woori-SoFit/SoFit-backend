# Implementation Plan: Admin 인증 방식 리팩토링

## Overview

sofit-admin 모듈의 관리자 인증 시스템을 리팩토링한다. SecurityContextHolder를 단일 인증 정보 소스로 통일하고, 세션 보안을 강화하며, 역할 기반 접근 제어를 세분화한다. 기존 session.setAttribute 기반 이중 저장을 제거하고 Spring Security 메커니즘만 활용하도록 변경한다.

## Tasks

- [x] 1. 에러코드 추가 및 AccessDeniedHandler 생성
  - [x] 1.1 AdminAuthErrorCode에 ACCESS_DENIED, CONCURRENT_LOGIN 에러코드 추가
    - ACCESS_DENIED(HttpStatus.FORBIDDEN, "COMMON4003", "권한이 없습니다.")
    - CONCURRENT_LOGIN(HttpStatus.CONFLICT, "AUTH4091", "이미 다른 기기에서 로그인되어 있습니다.")
    - _Requirements: 5.1, 7.5_

  - [x] 1.2 CustomAccessDeniedHandler 생성
    - `sofit-admin/src/main/java/com/sofit/admin/global/config/CustomAccessDeniedHandler.java` 생성
    - AccessDeniedHandler 구현, HTTP 403 + JSON 공통 응답 포맷 반환
    - 응답 형식: {"isSuccess": false, "code": "COMMON4003", "message": "권한이 없습니다.", "result": null}
    - _Requirements: 7.5_

- [x] 2. SecurityConfig 리팩토링
  - [x] 2.1 SecurityConfig에 세션 고정 공격 방지 및 동시 로그인 제한 설정 추가
    - sessionFixation().newSession() 설정 추가
    - maximumSessions(1) + maxSessionsPreventsLogin(true) 설정 추가
    - SessionCreationPolicy.IF_REQUIRED 유지
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.3_

  - [x] 2.2 SecurityConfig에 역할 기반 접근 제어 세분화 및 AccessDeniedHandler 등록
    - /api/admin/manager/loan-applications/*/approve → ADMIN_BANK_MANAGER only
    - /api/admin/dev/batch/s-grade → ADMIN_DEV only
    - /api/admin/dev/logs/api → ADMIN_DEV only
    - 세분화된 규칙을 /api/admin/** 전체 허용 규칙보다 먼저 배치
    - CustomAccessDeniedHandler를 exceptionHandling에 등록
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5, 7.6_

- [x] 3. CookieConfig 생성 (프로파일별 쿠키 설정)
  - [x] 3.1 CookieConfig 클래스 생성
    - `sofit-admin/src/main/java/com/sofit/admin/global/config/CookieConfig.java` 생성
    - @Profile("release"): secure=true, httpOnly=true, sameSite=Strict
    - @Profile({"local", "dev"}): secure=false, httpOnly=true, sameSite=Lax
    - DefaultCookieSerializer를 사용한 CookieSerializer Bean 등록
    - _Requirements: 6.1, 6.2, 6.3, 6.4_

- [ ] 4. Checkpoint - 보안 설정 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. AdminAuthServiceImpl 리팩토링
  - [x] 5.1 login() 메서드에서 session.setAttribute 제거
    - session.setAttribute("userId", ...) 제거
    - session.setAttribute("role", ...) 제거
    - session.setAttribute("loginTime", ...) 제거
    - SecurityContext 생성 및 세션 저장 로직은 유지 (SPRING_SECURITY_CONTEXT_KEY)
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 5.2 findMe() 메서드 리팩토링 - HttpSession 파라미터 제거 및 SecurityContextHolder 활용
    - AdminAuthService 인터페이스: findMe(HttpSession session) → findMe()로 시그니처 변경
    - AdminAuthServiceImpl: SecurityContextHolder.getContext().getAuthentication()에서 userId 추출
    - Authentication null 또는 principal null 시 BaseException(SESSION_EXPIRED) 발생
    - principal이 Long 타입 변환 불가 시 BaseException(SESSION_EXPIRED) 발생
    - 사용자 status가 INACTIVE이면 BaseException(USER_NOT_FOUND) 발생
    - _Requirements: 1.4, 1.5, 2.1, 2.2, 2.4, 2.5, 2.6, 3.1, 3.2, 3.3_

  - [ ]* 5.3 AdminAuthServiceImpl 단위 테스트 작성
    - login() 성공: SecurityContext에 올바른 Authentication 설정 확인
    - login() 성공: session.setAttribute("userId"/role/loginTime) 미호출 확인
    - findMe() 성공: SecurityContext에서 userId 추출 → 사용자 조회 → 응답 반환
    - findMe() 실패: Authentication null, principal null, principal 타입 불일치, 사용자 미존재, INACTIVE
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 2.2, 2.4, 2.5, 2.6_

- [x] 6. Controller 및 Docs 인터페이스 수정
  - [x] 6.1 AdminAuthController에서 findMe() HttpSession 파라미터 제거
    - findMe(HttpSession session) → findMe()로 변경
    - adminAuthService.findMe(session) → adminAuthService.findMe()로 호출 변경
    - _Requirements: 2.3_

  - [x] 6.2 AdminAuthControllerDocs에서 findMe() HttpSession 파라미터 제거
    - findMe(HttpSession session) → findMe()로 시그니처 변경
    - jakarta.servlet.http.HttpSession import 제거 (findMe에서만 사용 시)
    - _Requirements: 2.3_

- [x] 7. Checkpoint - 핵심 리팩토링 완료 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. 프로퍼티 기반 테스트 작성
  - [ ]* 8.1 Property 1 테스트: 로그인 후 SecurityContext Authentication 구성 정확성
    - **Property 1: 로그인 후 SecurityContext Authentication 구성 정확성**
    - **Validates: Requirements 1.2, 1.3**
    - jqwik 사용, 임의의 Long userId + 관리자 역할(3종 중 택 1) 생성
    - 검증: Authentication.principal == userId, authorities에 역할 name() 포함, authorities 크기 1

  - [ ]* 8.2 Property 2 테스트: SecurityContext에서 userId 추출 후 사용자 조회 일관성
    - **Property 2: SecurityContext에서 userId 추출 후 사용자 조회 일관성**
    - **Validates: Requirements 1.4, 2.2, 3.2**
    - jqwik 사용, 임의의 Long userId + 유효한 User 엔티티(ACTIVE 상태, 관리자 역할) 생성
    - 검증: findMe() 반환값의 userId가 SecurityContext에 설정된 값과 동일

  - [ ]* 8.3 Property 3 테스트: 비정상 principal 타입에 대한 예외 발생
    - **Property 3: 비정상 principal 타입에 대한 예외 발생**
    - **Validates: Requirements 2.5**
    - jqwik 사용, Long이 아닌 임의의 객체 (String, Integer, Double, Object 등) 생성
    - 검증: findMe() 호출 시 BaseException(SESSION_EXPIRED) 발생

- [ ] 9. 통합 테스트 작성
  - [ ]* 9.1 역할 기반 접근 제어 통합 테스트
    - MockMvc + @WithMockUser 활용
    - ADMIN_BANK_TELLER가 /api/admin/manager/loan-applications/1/approve 접근 시 403 확인
    - ADMIN_DEV가 /api/admin/dev/batch/s-grade 접근 시 200 확인
    - ADMIN_BANK_MANAGER가 /api/admin/dev/logs/api 접근 시 403 확인
    - 403 응답 JSON 포맷 확인 (isSuccess, code, message)
    - _Requirements: 7.2, 7.3, 7.4, 7.5, 7.6_

  - [ ]* 9.2 세션 고정 공격 방지 및 동시 로그인 제한 통합 테스트
    - 로그인 전후 세션 ID 변경 확인
    - 동일 계정 두 번째 로그인 시도 시 차단 확인
    - _Requirements: 4.1, 4.3, 5.1, 5.2_

- [ ] 10. Final checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 태스크는 특정 요구사항을 참조하여 추적 가능성을 보장
- Checkpoints에서 증분 검증 수행
- Property tests는 jqwik 라이브러리를 사용하여 설계 문서의 Correctness Properties를 검증
- 단위 테스트는 JUnit 5 + Mockito 사용
- 통합 테스트는 MockMvc + Spring Security Test 사용
- 세션 고정 공격 방지는 SecurityConfig의 sessionFixation().newSession()이 자동 처리하므로 서비스 코드에서 session.invalidate()를 직접 호출하지 않음

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "3.1"] },
    { "id": 1, "tasks": ["2.1", "2.2"] },
    { "id": 2, "tasks": ["5.1", "5.2"] },
    { "id": 3, "tasks": ["5.3", "6.1", "6.2"] },
    { "id": 4, "tasks": ["8.1", "8.2", "8.3"] },
    { "id": 5, "tasks": ["9.1", "9.2"] }
  ]
}
```
