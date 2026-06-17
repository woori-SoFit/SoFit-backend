# Design Document: KYC & PIN 인증 API

## Architecture Overview

KYC 사업자등록번호 인증과 금융인증서 PIN 인증을 처리하는 auth 도메인 설계이다. 두 API 모두 비로그인 상태에서 임시 세션을 사용하며, External Mock 서버와 RestTemplate으로 통신한다.

```
┌─────────────┐     ┌──────────────────┐     ┌───────────────────┐     ┌──────────────────┐
│   Client    │────▶│  AuthController  │────▶│   AuthService     │────▶│ ExternalMockClient│
│             │◀────│                  │◀────│                   │◀────│  (RestTemplate)   │
└─────────────┘     └──────────────────┘     └───────────────────┘     └──────────────────┘
                                                      │                          │
                                                      ▼                          ▼
                                             ┌─────────────────┐      ┌──────────────────┐
                                             │BusinessProfile   │      │  External Mock   │
                                             │  Repository      │      │    Server        │
                                             └─────────────────┘      └──────────────────┘
```

## Components

### 1. Controller Layer

**AuthController** (`domain/auth/controller/AuthController.java`)
- `POST /api/auth/signup/business-verification` → KYC 사업자 인증
- `POST /api/auth/financial-certificate/verify` → PIN 인증

### 2. Service Layer

**AuthService** (interface: `domain/auth/service/AuthService.java`)
- `verifyBusiness(BusinessVerificationRequest request)` → KYC 인증 처리
- `verifyFinancialCertificate(FinancialCertVerifyRequest request)` → PIN 인증 처리

**AuthServiceImpl** (`domain/auth/service/AuthServiceImpl.java`)
- External Mock 호출 → 응답 처리 → (KYC만) DB 저장 → 응답 반환

### 3. External Mock Client

**ExternalMockClient** (`domain/auth/service/ExternalMockClient.java`)
- RestTemplate을 사용하여 External Mock 서버와 통신
- KYC 호출: `POST {EXTERNAL_MOCK_URL}/ext/kyc/verify`
- PIN 호출: `POST {EXTERNAL_MOCK_URL}/ext/financial-certs/verify`
- 통신 오류 시 적절한 예외 변환

### 4. Configuration

**ExternalMockConfig** (`global/config/ExternalMockConfig.java`)
- `@Value("${external.mock.url}")` 로 환경변수 주입
- RestTemplate Bean 등록

**SecurityConfig** (`global/config/SecurityConfig.java`)
- `/api/auth/**` 경로 permitAll 설정
- 세션 생성 정책: IF_REQUIRED (임시 세션)

### 5. Entity

**BusinessProfile** (sofit-common, 확장)
- User와 N:1 관계 (`@ManyToOne`)
- status: `PENDING`, `VERIFIED`, `FAILED`
- KYC 인증 결과 필드 매핑

### 6. Exception & Code

**AuthErrorCode** (`domain/auth/exception/AuthErrorCode.java`)
- `AUTH4001` (401): PIN 번호 불일치
- `AUTH4004` (404): 사업자등록번호 미등록/폐업
- `AUTH4005` (404): 금융인증서 미등록
- `AUTH4006` (400): 입력값 형식 오류
- `AUTH5001` (502): External Mock 통신 오류

**AuthSuccessCode** (`domain/auth/exception/AuthSuccessCode.java`)
- `AUTH2001` (200): 사업자 인증 성공
- `AUTH2006` (200): 금융인증서 본인인증 성공

## Interfaces

### Request DTOs

```java
// domain/auth/dto/request/BusinessVerificationRequest.java
public record BusinessVerificationRequest(
    @NotBlank String businessNumber  // 하이픈 없는 10자리 숫자
) {}

// domain/auth/dto/request/FinancialCertVerifyRequest.java
public record FinancialCertVerifyRequest(
    @NotBlank String phoneNumber,  // 하이픈 없는 11자리 숫자
    @NotBlank String pin           // 6자리 숫자
) {}
```

### Response DTOs

```java
// domain/auth/dto/response/BusinessVerificationResponse.java
public record BusinessVerificationResponse(
    Long kycId,
    String businessNumber,
    String representativeName,
    String businessName,
    String businessType,
    String openDate,
    boolean isValid,
    LocalDateTime verifiedAt
) {}

// domain/auth/dto/response/FinancialCertVerifyResponse.java
public record FinancialCertVerifyResponse(
    Long certId,
    String certNumber,
    String holderName,
    String phoneNumber,
    String status,
    LocalDateTime verifiedAt
) {}
```

### External Mock Request/Response DTOs

```java
// domain/auth/dto/request/ExternalKycRequest.java
public record ExternalKycRequest(
    String businessNumber
) {}

// domain/auth/dto/response/ExternalKycResponse.java
public record ExternalKycResponse(
    String businessNumber,
    String representativeName,
    String businessCategory,
    String businessType,
    String businessName,
    String businessAddress,
    String openDate,
    String status  // "ACTIVE", "CLOSED", "NOT_FOUND"
) {}

// domain/auth/dto/request/ExternalFinancialCertRequest.java
public record ExternalFinancialCertRequest(
    String phoneNumber,
    String pin
) {}

// domain/auth/dto/response/ExternalFinancialCertResponse.java
public record ExternalFinancialCertResponse(
    Long certId,
    String certNumber,
    String holderName,
    String phoneNumber,
    String status,       // "VERIFIED", "PIN_MISMATCH", "NOT_FOUND"
    LocalDateTime verifiedAt
) {}
```

### Service Interface

```java
// domain/auth/service/AuthService.java
public interface AuthService {
    BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request, Long userId);
    FinancialCertVerifyResponse verifyFinancialCertificate(FinancialCertVerifyRequest request);
}
```

## Data Models

### BusinessProfile Entity (확장)

```java
@Entity
@Table(name = "business_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusinessProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_profile_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "business_number", nullable = false, length = 10)
    private String businessNumber;

    @Column(name = "representative_name")
    private String representativeName;

    @Column(name = "business_category")
    private String businessCategory;

    @Column(name = "business_type")
    private String businessType;

    @Column(name = "business_name")
    private String businessName;

    @Column(name = "business_address")
    private String businessAddress;

    @Column(name = "open_date")
    private String openDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BusinessProfileStatus status;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "is_mybiz_connected", nullable = false)
    private boolean isMybizConnected = false;

    @Column(name = "mybiz_connected_at")
    private LocalDateTime mybizConnectedAt;

    @Column(name = "mydata_all_agreed", nullable = false)
    private boolean mydataAllAgreed = false;

    @Column(name = "mydata_all_agreed_at")
    private LocalDateTime mydataAllAgreedAt;

    // 정적 팩토리 메서드
    public static BusinessProfile createVerified(User user, ExternalKycResponse kycResponse) {
        BusinessProfile profile = new BusinessProfile();
        profile.user = user;
        profile.businessNumber = kycResponse.businessNumber();
        profile.representativeName = kycResponse.representativeName();
        profile.businessCategory = kycResponse.businessCategory();
        profile.businessType = kycResponse.businessType();
        profile.businessName = kycResponse.businessName();
        profile.businessAddress = kycResponse.businessAddress();
        profile.openDate = kycResponse.openDate();
        profile.status = BusinessProfileStatus.VERIFIED;
        profile.verifiedAt = LocalDateTime.now();
        return profile;
    }
}
```

### BusinessProfileStatus Enum

```java
// domain/auth/enums/BusinessProfileStatus.java
public enum BusinessProfileStatus {
    PENDING,
    VERIFIED,
    FAILED
}
```

## Sequence Diagrams

### KYC 사업자 인증 플로우

```
Client → AuthController: POST /api/auth/signup/business-verification
AuthController → AuthController: validate businessNumber (10자리 숫자)
AuthController → AuthServiceImpl: verifyBusiness(request, userId)
AuthServiceImpl → ExternalMockClient: callKycVerify(businessNumber)
ExternalMockClient → External Mock: POST /ext/kyc/verify
External Mock → ExternalMockClient: KYC 결과 반환

[유효한 사업자]
ExternalMockClient → AuthServiceImpl: ExternalKycResponse (status=ACTIVE)
AuthServiceImpl → BusinessProfileRepository: save(BusinessProfile.createVerified(...))
AuthServiceImpl → AuthController: BusinessVerificationResponse
AuthController → Client: ApiResponse(AUTH2001, result)

[폐업/미등록]
ExternalMockClient → AuthServiceImpl: ExternalKycResponse (status=CLOSED|NOT_FOUND)
AuthServiceImpl → AuthServiceImpl: throw BaseException(AUTH4004)
GlobalExceptionHandler → Client: ApiResponse(AUTH4004, 404)
```

### PIN 인증 플로우

```
Client → AuthController: POST /api/auth/financial-certificate/verify
AuthController → AuthController: validate phoneNumber(11자리), pin(6자리)
AuthController → AuthServiceImpl: verifyFinancialCertificate(request)
AuthServiceImpl → ExternalMockClient: callFinancialCertVerify(phoneNumber, pin)
ExternalMockClient → External Mock: POST /ext/financial-certs/verify
External Mock → ExternalMockClient: 인증 결과 반환

[인증 성공]
ExternalMockClient → AuthServiceImpl: ExternalFinancialCertResponse (status=VERIFIED)
AuthServiceImpl → AuthController: FinancialCertVerifyResponse
AuthController → Client: ApiResponse(AUTH2006, result)

[PIN 불일치]
ExternalMockClient → AuthServiceImpl: ExternalFinancialCertResponse (status=PIN_MISMATCH)
AuthServiceImpl → AuthServiceImpl: throw BaseException(AUTH4001)
GlobalExceptionHandler → Client: ApiResponse(AUTH4001, 401)

[인증서 미등록]
ExternalMockClient → AuthServiceImpl: ExternalFinancialCertResponse (status=NOT_FOUND)
AuthServiceImpl → AuthServiceImpl: throw BaseException(AUTH4005)
GlobalExceptionHandler → Client: ApiResponse(AUTH4005, 404)
```

## Error Handling

| 상황 | ErrorCode | HTTP Status | 메시지 |
|------|-----------|-------------|--------|
| 사업자등록번호 미등록/폐업 | AUTH4004 | 404 | 일치하는 사업자등록번호를 찾을 수 없습니다. |
| PIN 번호 불일치 | AUTH4001 | 401 | PIN 번호가 올바르지 않습니다. |
| 금융인증서 미등록 | AUTH4005 | 404 | 등록된 금융인증서를 찾을 수 없습니다. |
| 입력값 형식 오류 | AUTH4006 | 400 | 입력값 형식이 올바르지 않습니다. |
| External Mock 통신 오류 | AUTH5001 | 502 | 외부 인증 서버와 통신 중 오류가 발생했습니다. |

### 예외 처리 흐름

1. 입력 검증 실패 → `@Valid` + `MethodArgumentNotValidException` → GlobalExceptionHandler
2. 비즈니스 로직 예외 → `throw new BaseException(AuthErrorCode.XXX)` → GlobalExceptionHandler
3. 외부 통신 오류 → `RestClientException` catch → `throw new BaseException(AUTH5001)`

## File Structure

```
sofit-user/src/main/java/com/sofit/user/
├── domain/auth/
│   ├── controller/
│   │   └── AuthController.java
│   ├── service/
│   │   ├── AuthService.java
│   │   ├── AuthServiceImpl.java
│   │   └── ExternalMockClient.java
│   ├── converter/
│   │   └── AuthConverter.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── BusinessVerificationRequest.java
│   │   │   ├── FinancialCertVerifyRequest.java
│   │   │   ├── ExternalKycRequest.java
│   │   │   └── ExternalFinancialCertRequest.java
│   │   └── response/
│   │       ├── BusinessVerificationResponse.java
│   │       ├── FinancialCertVerifyResponse.java
│   │       ├── ExternalKycResponse.java
│   │       └── ExternalFinancialCertResponse.java
│   ├── enums/
│   │   └── BusinessProfileStatus.java
│   └── exception/
│       ├── AuthErrorCode.java
│       └── AuthSuccessCode.java
├── global/
│   └── config/
│       ├── ExternalMockConfig.java
│       └── SecurityConfig.java

sofit-common/src/main/java/com/sofit/common/
├── entity/
│   ├── BusinessProfile.java (확장)
│   └── User.java (확장: BusinessProfile 관계 추가)
└── repository/
    └── BusinessProfileRepository.java
```

## Configuration

### application.yml 추가 설정

```yaml
# External Mock 서버
external:
  mock:
    url: ${EXTERNAL_MOCK_URL:http://localhost:9090}
```

### .env.example 추가

```
# External Mock
EXTERNAL_MOCK_URL=http://localhost:9090
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: 입력 검증 정확성

*For any* 문자열 입력에 대해, businessNumber는 정확히 10자리 숫자(`^\d{10}$`)인 경우에만, phoneNumber는 정확히 11자리 숫자(`^\d{11}$`)인 경우에만, pin은 정확히 6자리 숫자(`^\d{6}$`)인 경우에만 검증을 통과하고, 그 외 모든 입력은 거부되어야 한다.

**Validates: Requirements 1.6, 2.7, 2.8**

### Property 2: KYC 성공 시 BusinessProfile VERIFIED 저장

*For any* 유효한 사업자 정보(status=ACTIVE)를 반환하는 External Mock 응답에 대해, AuthService는 항상 BusinessProfile을 status=VERIFIED, verifiedAt이 non-null인 상태로 DB에 저장해야 한다.

**Validates: Requirements 1.3**

### Property 3: KYC 성공 응답 형식 보장

*For any* 유효한 KYC 인증 결과에 대해, 응답은 항상 코드 AUTH2001을 포함하고, result에 kycId, businessNumber, representativeName, businessName, businessType, openDate, isValid, verifiedAt 필드가 모두 존재해야 한다.

**Validates: Requirements 1.4**

### Property 4: PIN 인증 DB 미저장 보장

*For any* PIN 인증 요청(성공/실패 무관)에 대해, 인증 처리 전후로 DB의 레코드 수가 변하지 않아야 한다.

**Validates: Requirements 2.6**

### Property 5: PIN 성공 응답 형식 보장

*For any* 유효한 금융인증서 인증 결과(status=VERIFIED)에 대해, 응답은 항상 코드 AUTH2006을 포함하고, result에 certId, certNumber, holderName, phoneNumber, status, verifiedAt 필드가 모두 존재해야 한다.

**Validates: Requirements 2.3**

### Property 6: AuthErrorCode → ApiResponse 변환 일관성

*For any* AuthErrorCode 값에 대해, BaseException을 throw하면 GlobalExceptionHandler가 항상 isSuccess=false, 해당 에러의 code와 message를 포함하는 ApiResponse를 반환해야 한다.

**Validates: Requirements 5.3**
