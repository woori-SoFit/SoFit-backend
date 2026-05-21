# Tasks

## Task 1: build.gradle 의존성 추가 및 PasswordEncoderConfig 생성

- [x] `sofit-admin/build.gradle`에 `spring-boot-starter-data-redis`, `spring-session-data-redis` 의존성 추가
- [x] `sofit-admin/src/main/java/com/sofit/admin/global/config/PasswordEncoderConfig.java` 생성
  - `@Configuration` 클래스
  - `BCryptPasswordEncoder`를 `PasswordEncoder` 빈으로 등록

## Task 2: auth 도메인 DTO 생성

- [x] `sofit-admin/src/main/java/com/sofit/admin/domain/auth/dto/request/AdminLoginRequest.java` 생성
  - class 타입 사용
  - `loginId`: `@NotBlank(message = "아이디를 입력해주세요.")`
  - `password`: `@NotBlank(message = "비밀번호를 입력해주세요.")`
  - Lombok `@Getter`, `@NoArgsConstructor` 적용
- [x] `sofit-admin/src/main/java/com/sofit/admin/domain/auth/dto/response/AdminLoginResponse.java` 생성
  - record 타입 사용
  - 필드: `userId`(Long), `name`(String), `role`(String)

## Task 3: auth 도메인 ErrorCode / SuccessCode 생성

- [x] `sofit-admin/src/main/java/com/sofit/admin/domain/auth/exception/AdminAuthErrorCode.java` 생성
  - `BaseErrorCode` 인터페이스 구현
  - `LOGIN_FAILED(HttpStatus.BAD_REQUEST, "AUTH4001", "아이디 또는 비밀번호가 올바르지 않습니다.")`
- [x] `sofit-admin/src/main/java/com/sofit/admin/domain/auth/exception/AdminAuthSuccessCode.java` 생성
  - `BaseSuccessCode` 인터페이스 구현
  - `LOGIN_SUCCESS(HttpStatus.OK, "AUTH2001", "로그인에 성공했습니다.")`

## Task 4: AdminAuthConverter 생성

- [x] `sofit-admin/src/main/java/com/sofit/admin/domain/auth/converter/AdminAuthConverter.java` 생성
  - `toLoginResponse(User user)` 정적 메서드: User 엔티티 → AdminLoginResponse 변환
  - role은 `user.getRole().name()`으로 문자열 변환

## Task 5: AdminAuthService 인터페이스 및 구현체 생성

- [x] `sofit-admin/src/main/java/com/sofit/admin/domain/auth/service/AdminAuthService.java` 인터페이스 생성
  - `AdminLoginResponse login(AdminLoginRequest request, HttpSession session)` 메서드 정의
- [x] `sofit-admin/src/main/java/com/sofit/admin/domain/auth/service/AdminAuthServiceImpl.java` 구현체 생성
  - `@Service` 어노테이션
  - `UserRepository`, `PasswordEncoder` 주입 (생성자 주입)
  - 로그인 로직 구현:
    1. `loginId`로 User 조회 → 없으면 `throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED)`
    2. User의 status가 INACTIVE이면 `throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED)`
    3. User의 role이 USER이면 `throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED)`
    4. `passwordEncoder.matches()`로 비밀번호 검증 → 불일치 시 `throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED)`
    5. HttpSession에 `userId`, `role`, `loginTime` 저장
    6. `AdminAuthConverter.toLoginResponse(user)` 반환

## Task 6: AdminAuthController 및 ControllerDocs 생성

- [x] `sofit-admin/src/main/java/com/sofit/admin/domain/auth/controller/AdminAuthControllerDocs.java` 인터페이스 생성
  - Swagger `@Operation`, `@ApiResponse` 어노테이션으로 API 문서화
  - `login()` 메서드 시그니처 정의
- [x] `sofit-admin/src/main/java/com/sofit/admin/domain/auth/controller/AdminAuthController.java` 생성
  - `@RestController`, `@RequestMapping("/api/admin/auth")`
  - `AdminAuthControllerDocs` implements
  - `@PostMapping("/login")` 엔드포인트
  - `@Valid @RequestBody AdminLoginRequest`, `HttpSession` 파라미터
  - `AdminAuthService` 주입 (생성자 주입)
  - 응답: `ApiResponse.onSuccess(AdminAuthSuccessCode.LOGIN_SUCCESS, response)`

## Task 7: SecurityConfig 수정

- [x] `sofit-admin/src/main/java/com/sofit/admin/global/config/SecurityConfig.java` 수정
  - `/api/admin/auth/login` 엔드포인트를 `permitAll()`로 설정
  - 나머지 요청은 `authenticated()`로 변경
  - 기존 Swagger UI 허용 유지

## Task 8: 컴파일 확인

- [x] `./gradlew :sofit-admin:compileJava` 실행하여 컴파일 오류 없는지 확인
- [x] 오류 발생 시 수정
