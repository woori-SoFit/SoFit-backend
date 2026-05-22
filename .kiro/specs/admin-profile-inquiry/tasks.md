# Implementation Plan: 관리자(은행원) 정보 조회 (Admin Profile Inquiry)

## Overview

로그인한 관리자(은행원)가 `GET /api/admin/auth/me` 엔드포인트를 통해 자신의 프로필 정보(이름, 로그인 아이디, 전화번호, 역할)를 조회하는 API를 구현한다. 기존 AdminAuth 도메인에 메서드를 추가하는 방식으로 구현하며, CustomAuthenticationEntryPoint를 통해 인증 실패 시 JSON 에러 응답을 제공한다.

## Tasks

- [x] 1. Response DTO 및 코드 정의
  - [x] 1.1 AdminMeResponse record 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/auth/dto/response/AdminMeResponse.java` 생성
    - 필드: `name`, `loginId`, `phoneNumber`, `role` (모두 String)
    - record 타입으로 작성
    - _Requirements: 1.2_

  - [x] 1.2 AdminAuthSuccessCode에 ME_SUCCESS 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/auth/exception/AdminAuthSuccessCode.java` 수정
    - `ME_SUCCESS(HttpStatus.OK, "ADMIN2001", "관리자 정보 조회에 성공했습니다.")` 추가
    - _Requirements: 1.1_

  - [x] 1.3 AdminAuthErrorCode에 SESSION_EXPIRED, USER_NOT_FOUND 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/auth/exception/AdminAuthErrorCode.java` 수정
    - `SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH4011", "세션이 만료되었습니다. 다시 로그인해 주세요.")` 추가
    - `USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH4041", "요청한 리소스를 찾을 수 없습니다.")` 추가
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 2. Converter 수정
  - [x] 2.1 AdminAuthConverter에 formatPhoneNumber, toMeResponse 메서드 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/auth/converter/AdminAuthConverter.java` 수정
    - `formatPhoneNumber(String phoneNumber)` 메서드 구현
      - null/빈 문자열 → 빈 문자열 반환
      - 11자리 → `NNN-NNNN-NNNN` 형식
      - 10자리 → `NN-NNNN-NNNN` 형식
      - 그 외 → 원본 그대로 반환
    - `toMeResponse(User user)` 메서드 구현
      - User 엔티티의 name, loginId, phoneNumber(포맷팅), role을 변환하여 AdminMeResponse 생성
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 1.2_

  - [ ]* 2.2 Property 1: 유효한 전화번호 포맷팅 라운드트립 테스트
    - **Property 1: 유효한 전화번호 포맷팅 라운드트립**
    - **Validates: Requirements 2.1, 2.2, 2.3**
    - jqwik을 사용하여 10자리/11자리 숫자 문자열에 대해 포맷팅 결과에서 하이픈 제거 시 원본과 동일한지(라운드트립), 형식 패턴이 올바른지 검증
    - 테스트 위치: `sofit-admin/src/test/java/com/sofit/admin/domain/auth/converter/AdminAuthConverterPropertyTest.java`

  - [ ]* 2.3 Property 2: 유효하지 않은 길이의 전화번호 원본 보존 테스트
    - **Property 2: 유효하지 않은 길이의 전화번호 원본 보존**
    - **Validates: Requirements 2.5**
    - jqwik을 사용하여 10/11자리가 아닌 문자열에 대해 포맷팅 결과가 원본과 동일한지 검증
    - 테스트 위치: `sofit-admin/src/test/java/com/sofit/admin/domain/auth/converter/AdminAuthConverterPropertyTest.java`

  - [ ]* 2.4 AdminAuthConverter 단위 테스트 작성
    - 전화번호 포맷팅: 11자리, 10자리, null, 빈 문자열, 비정상 길이(9자리, 12자리)
    - toMeResponse 변환 정합성 확인 (name, loginId, phoneNumber 포맷팅, role 매핑)
    - 테스트 위치: `sofit-admin/src/test/java/com/sofit/admin/domain/auth/converter/AdminAuthConverterTest.java`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 3. Checkpoint - Converter 구현 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Service 수정
  - [x] 4.1 AdminAuthService 인터페이스 및 AdminAuthServiceImpl에 findMe 메서드 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/auth/service/AdminAuthService.java` 수정
      - `AdminMeResponse findMe(HttpSession session)` 메서드 선언 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/auth/service/AdminAuthServiceImpl.java` 수정
      - 세션에서 `userId` 속성 추출
      - `UserRepository.findById(userId)`로 사용자 조회
      - 사용자 미존재 시 `BaseException(AdminAuthErrorCode.USER_NOT_FOUND)` throw
      - 사용자 상태가 INACTIVE이면 `BaseException(AdminAuthErrorCode.USER_NOT_FOUND)` throw
      - `AdminAuthConverter.toMeResponse(user)` 호출하여 응답 반환
    - _Requirements: 1.1, 1.2, 3.2, 3.3_

  - [ ]* 4.2 AdminAuthServiceImpl 단위 테스트 작성
    - Mockito로 UserRepository mock 처리
    - 정상 조회 (ACTIVE 관리자) 시나리오
    - 사용자 미존재 시 BaseException(USER_NOT_FOUND) 발생 확인
    - INACTIVE 사용자 시 BaseException(USER_NOT_FOUND) 발생 확인
    - 세션에서 userId 추출 로직 검증
    - 테스트 위치: `sofit-admin/src/test/java/com/sofit/admin/domain/auth/service/AdminAuthServiceImplTest.java`
    - _Requirements: 3.2, 3.3_

- [x] 5. Controller 및 Swagger 수정
  - [x] 5.1 AdminAuthControllerDocs에 findMe 메서드 Swagger 문서 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/auth/controller/AdminAuthControllerDocs.java` 수정
    - `@Operation(summary = "관리자 내 정보 조회", description = "로그인한 관리자의 프로필 정보를 조회합니다.")` 어노테이션
    - `@ApiResponses`에 200(조회 성공), 401(세션 만료), 404(사용자 미존재) 응답 코드 명시
    - `ApiResponse<AdminMeResponse> findMe(HttpSession session)` 메서드 선언
    - _Requirements: 5.1, 5.2_

  - [x] 5.2 AdminAuthController에 GET /me 엔드포인트 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/auth/controller/AdminAuthController.java` 수정
    - `@GetMapping("/me")` 엔드포인트 구현
    - `HttpSession` 파라미터로 세션 수신
    - `adminAuthService.findMe(session)` 호출 후 `ApiResponse.onSuccess(AdminAuthSuccessCode.ME_SUCCESS, response)` 반환
    - _Requirements: 1.1, 1.2_

  - [ ]* 5.3 AdminAuthController MockMvc 테스트 작성
    - MockMvc를 사용한 `GET /api/admin/auth/me` 엔드포인트 테스트
    - 성공 응답 시 HTTP 200, 응답 코드 `ADMIN2001`, 메시지 확인
    - 응답 body에 name, loginId, phoneNumber, role 필드 존재 확인
    - 테스트 위치: `sofit-admin/src/test/java/com/sofit/admin/domain/auth/controller/AdminAuthControllerTest.java`
    - _Requirements: 1.1, 1.2, 5.1_

- [x] 6. CustomAuthenticationEntryPoint 구현
  - [x] 6.1 CustomAuthenticationEntryPoint 클래스 생성
    - `sofit-admin/src/main/java/com/sofit/admin/global/config/CustomAuthenticationEntryPoint.java` 생성
    - `AuthenticationEntryPoint` 인터페이스 구현
    - `commence()` 메서드에서:
      - `response.setStatus(HttpServletResponse.SC_UNAUTHORIZED)` 설정
      - `response.setContentType("application/json;charset=UTF-8")` 설정
      - ObjectMapper로 공통 에러 응답 포맷 JSON 작성 (isSuccess: false, code: "AUTH4011", message, result: null)
    - _Requirements: 4.1, 4.2_

  - [x] 6.2 SecurityConfig에 CustomAuthenticationEntryPoint 등록
    - `sofit-admin/src/main/java/com/sofit/admin/global/config/SecurityConfig.java` 수정
    - `CustomAuthenticationEntryPoint`를 Bean으로 주입
    - `.exceptionHandling(ex -> ex.authenticationEntryPoint(customAuthenticationEntryPoint))` 추가
    - _Requirements: 3.1, 4.1_

  - [ ]* 6.3 CustomAuthenticationEntryPoint 테스트 작성
    - MockMvc를 사용하여 세션 없이 보호된 엔드포인트 호출
    - HTTP 401 상태 코드, Content-Type: application/json 확인
    - 응답 본문에 isSuccess: false, code: AUTH4011, message, result: null 확인
    - 테스트 위치: `sofit-admin/src/test/java/com/sofit/admin/global/config/CustomAuthenticationEntryPointTest.java`
    - _Requirements: 3.1, 4.1, 4.2_

- [x] 7. Final checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 태스크는 특정 요구사항을 참조하여 추적 가능
- Checkpoints에서 점진적 검증 수행
- Property 테스트는 jqwik 라이브러리를 사용하여 Correctness Properties를 검증
- 단위 테스트는 JUnit 5 + Mockito로 특정 예제와 엣지 케이스를 검증
- User 엔티티와 UserRepository는 sofit-common 모듈에 이미 존재
- 신규 파일은 AdminMeResponse record와 CustomAuthenticationEntryPoint만 생성
- 나머지는 모두 기존 파일 수정

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4"] },
    { "id": 3, "tasks": ["4.1"] },
    { "id": 4, "tasks": ["4.2", "5.1"] },
    { "id": 5, "tasks": ["5.2"] },
    { "id": 6, "tasks": ["5.3", "6.1"] },
    { "id": 7, "tasks": ["6.2"] },
    { "id": 8, "tasks": ["6.3"] }
  ]
}
```
