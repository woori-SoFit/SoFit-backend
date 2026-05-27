# Technical Design Document

## Overview

sofit-admin 모듈에 관리자 페이지 로그인 API(`POST /api/admin/auth/login`)를 구현한다. 기존 sofit-common 모듈의 User 엔티티, UserRepository, ApiResponse, BaseException 등을 재사용하며, admin 전용 auth 도메인 패키지를 새로 생성한다. Spring Security의 BCryptPasswordEncoder를 사용하여 비밀번호를 검증하고, Spring Session + Redis로 세션을 관리한다.

## Architecture

### 패키지 구조

```
sofit-admin/src/main/java/com/sofit/admin/
├── domain/
│   └── auth/                          # 신규 생성
│       ├── controller/
│       │   ├── AdminAuthController.java
│       │   └── AdminAuthControllerDocs.java
│       ├── dto/
│       │   ├── request/
│       │   │   └── AdminLoginRequest.java
│       │   └── response/
│       │       └── AdminLoginResponse.java
│       ├── service/
│       │   ├── AdminAuthService.java
│       │   └── AdminAuthServiceImpl.java
│       ├── converter/
│       │   └── AdminAuthConverter.java
│       └── exception/
│           ├── AdminAuthErrorCode.java
│           └── AdminAuthSuccessCode.java
└── global/
    └── config/
        ├── SecurityConfig.java        # 수정 (로그인 엔드포인트 permitAll, 나머지 인증 필요)
        └── PasswordEncoderConfig.java  # 신규 생성
```

### 시퀀스 다이어그램

```
Client → AdminAuthController → AdminAuthService → UserRepository → DB
                                      ↓
                              PasswordEncoder.matches()
                                      ↓
                              Session 생성 (Redis)
                                      ↓
                              AdminLoginResponse 반환
```

## Components

### 1. AdminLoginRequest (Request DTO)

```java
// class 사용 (Bean Validation 적용)
public class AdminLoginRequest {
    @NotBlank(message = "아이디를 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;
}
```

### 2. AdminLoginResponse (Response DTO)

```java
// record 사용 (불변 데이터 전달)
public record AdminLoginResponse(
    Long userId,
    String name,
    String role
) {}
```

### 3. AdminAuthService / AdminAuthServiceImpl

**핵심 로직:**
1. loginId로 User 조회 → 없으면 AUTH4001 에러
2. User의 status가 INACTIVE이면 AUTH4001 에러
3. User의 role이 USER이면 AUTH4001 에러 (관리자 권한 없음)
4. BCryptPasswordEncoder로 비밀번호 검증 → 불일치 시 AUTH4001 에러
5. HttpSession에 userId, role, loginTime 저장
6. AdminLoginResponse 반환

**보안 고려사항:**
- 아이디 미존재, 비밀번호 불일치, 권한 없음, 비활성 계정 모두 동일한 에러 메시지 반환 (정보 노출 방지)

### 4. AdminAuthController

```java
@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController implements AdminAuthControllerDocs {

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpSession session) {
        AdminLoginResponse response = adminAuthService.login(request, session);
        return ApiResponse.onSuccess(AdminAuthSuccessCode.LOGIN_SUCCESS, response);
    }
}
```

### 5. AdminAuthConverter

```java
public class AdminAuthConverter {
    public static AdminLoginResponse toLoginResponse(User user) {
        return new AdminLoginResponse(
            user.getUserId(),
            user.getName(),
            user.getRole().name()
        );
    }
}
```

### 6. AdminAuthErrorCode

```java
@Getter
@AllArgsConstructor
public enum AdminAuthErrorCode implements BaseErrorCode {
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "AUTH4001", "아이디 또는 비밀번호가 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

### 7. AdminAuthSuccessCode

```java
@Getter
@AllArgsConstructor
public enum AdminAuthSuccessCode implements BaseSuccessCode {
    LOGIN_SUCCESS(HttpStatus.OK, "AUTH2001", "로그인에 성공했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
```

### 8. PasswordEncoderConfig

```java
@Configuration
public class PasswordEncoderConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 9. SecurityConfig 수정

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/admin/auth/login").permitAll()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
        return http.build();
    }
}
```

## Data Model

### 사용하는 기존 엔티티 (sofit-common)

| 엔티티 | 테이블 | 용도 |
|--------|--------|------|
| User | users | 사용자 조회 (loginId, passwordHash, role, status) |

### 세션 저장 데이터 (Redis)

| 키 | 타입 | 설명 |
|----|------|------|
| userId | Long | 로그인한 사용자 ID |
| role | String | 사용자 역할 (ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV) |
| loginTime | LocalDateTime | 로그인 시각 |

## Error Handling

| 상황 | ErrorCode | HTTP Status | 메시지 |
|------|-----------|-------------|--------|
| 아이디 미존재 | AUTH4001 | 400 | 아이디 또는 비밀번호가 올바르지 않습니다. |
| 비밀번호 불일치 | AUTH4001 | 400 | 아이디 또는 비밀번호가 올바르지 않습니다. |
| USER 역할 (권한 없음) | AUTH4001 | 400 | 아이디 또는 비밀번호가 올바르지 않습니다. |
| INACTIVE 상태 | AUTH4001 | 400 | 아이디 또는 비밀번호가 올바르지 않습니다. |
| 입력값 누락/빈값 | COMMON4000 | 400 | 잘못된 요청입니다. |

## Dependencies

### 기존 의존성 (이미 build.gradle에 포함)
- `spring-boot-starter-web`
- `spring-boot-starter-security`
- `sofit-common` (User, UserRepository, ApiResponse, BaseException 등)

### 추가 필요 의존성
- `spring-boot-starter-data-redis` (Redis 세션 저장소)
- `spring-session-data-redis` (Spring Session Redis 연동)

## Testing

### API Dog 테스트 JSON

**성공 케이스:**
```json
{
  "loginId": "dev_admin",
  "password": "sofit1234!"
}
```

**실패 케이스 - 잘못된 비밀번호:**
```json
{
  "loginId": "dev_admin",
  "password": "wrongpassword"
}
```

**실패 케이스 - 빈 값:**
```json
{
  "loginId": "",
  "password": ""
}
```

**실패 케이스 - USER 권한:**
```json
{
  "loginId": "testuser1",
  "password": "sofit1234!"
}
```
