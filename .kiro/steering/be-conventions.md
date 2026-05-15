# be-conventions

# SoFit BE 개발 컨벤션

## 패키지 구조 (도메인형)

```
sofit-user/src/main/java/com/sofit/user/
├── domain/
│   ├── user/             # 내 정보 조회, 탈퇴
│   │   ├── controller/
│   │   ├── service/
│   │   ├── converter/
│   │   ├── enums/
│   │   ├── exception/
│   │   └── dto/
│   ├── auth/             # 로그인, 회원가입, PIN 인증
│   ├── loan/             # 대출 신청, 약정, 실행
│   │   ├── entity/       # Term, ConsentHistory (user 전용)
│   │   ├── repository/
│   │   └── ...
│   ├── mybiz/            # My Biz Data 수집/조회
│   ├── notification/     # 알림
│   │   ├── entity/       # Notification (user 전용)
│   │   ├── repository/
│   │   └── ...
│   └── report/           # 성장S등급 리포트
└── global/
    ├── config/
    └── filter/           # 세션 검증
	

sofit-admin/src/main/java/com/sofit/admin/
├── domain/
│   ├── loan/             # 대출 심사 처리
│   │   ├── entity/       # ScbInputFeature (admin 전용)
│   │   ├── repository/
│   │   └── ...
│   ├── product/          # 대출 상품 관리
│   └── dev/              # 사용자 관리, API 로그
│       ├── entity/       # ApiLog, BatchExecutionHistory, AccountStatusHistory
│       ├── repository/
│       └── ...
└── global/
    ├── config/
    ├── aop/              # API 로그 수집
    └── batch/            # 일일/월별 배치 Job
     
             
sofit-common/src/main/java/com/sofit/common/
├── entity/               # BaseEntity, 공통 사용 엔티티(User, BusinessProfile.. 등)
├── apiPayload/           # 공통 응답 포맷
├── repository/           # 공통 Entity Repository
└── util/
```

## 공통 응답 포맷

모든 API 응답은 아래 포맷을 따른다.

```json
{
  "isSuccess": true,
  "code": "COMMON2000",
  "message": "성공입니다.",
  "result": {}
}
```

### ErrorCode

```
COMMON5000 - 500: "서버 에러, 관리자에게 문의 바랍니다."
COMMON4000 - 400: "잘못된 요청입니다."
COMMON4001 - 401: "인증이 필요합니다."
COMMON4003 - 403: "권한이 없습니다."
COMMON4004 - 404: "요청한 리소스를 찾을 수 없습니다.
```

- 도메인별 ErrorCode는 해당 도메인 exception/ 안에 정의
- common의 GlobalExceptionHandler에서 일괄 처리
- 예외 발생 시 throw new BaseException([ErrorCode.XXX](http://errorcode.xxx/)) 형태 통일

## API URL 컨벤션

- 고객용: `/api/**`
- 은행원+개발자용: `/api/admin/**`
- 케밥 케이스: `/api/loan-applications/{applicationId}`
- Path variable: camelCase (`{applicationId}`, `{productId}`)

## 네이밍 컨벤션

```
# 클래스 네이밍
Controller   → UserController
Service      → UserService (interface) / UserServiceImpl (구현체)
Repository   → UserRepository
DTO          → UserCreateRequest, UserCreateResponse
Converter    → UserConverter
Entity       → User

# 메서드 네이밍
조회 (단건)  → findUser()
조회 (목록)  → findUsers()
생성         → createUser()
수정         → updateUser()
삭제         → deleteUser()
```

## DTO 분리 규칙

```
# Request/Response 명확히 분리
dto/
├── request/
│   └── UserCreateRequest.java
└── response/
    └── UserResponse.java

# Entity를 Controller 레이어까지 올리지 않는다
# Converter에서 Entity ↔ DTO 변환 처리
```

### **DTO 타입 선택 기준 (record vs class)**

- Response DTO → record 사용 (불변 데이터 전달 목적)
- Request DTO → class 사용 (Bean Validation 등 추가 로직이 붙는 경우 대응)

```
// Response → record
public record LoanProductResponse(Long id, String name) {}

// Request → class
public class LoanApplyRequest {
    @NotNull
    private Long productId;
}
```

## **Controller 작성 규칙**

### **응답 변수명**

Controller에서 Service 반환값을 담는 변수명은 response로 통일한다.

```
// ✅ 올바른 방식
public ApiResponse<LoanProductListResponse> getProducts() {
    LoanProductListResponse response = loanProductService.findProducts();
    return ApiResponse.onSuccess(LoanSuccessCode.LOAN_PRODUCT_LIST_OK, response);
}
```

### **List 응답 래핑**

목록 응답 시 List<DTO>를 직접 반환하지 않는다. DTO 안에 List를 감싸서 반환한다.

```
// ❌ 금지
public ApiResponse<List<LoanProductResponse>> getProducts() { ... }

// ✅ 올바른 방식
public ApiResponse<LoanProductListResponse> getProducts() {
    LoanProductListResponse response = loanProductService.findProducts();
    return ApiResponse.onSuccess(LoanSuccessCode.LOAN_PRODUCT_LIST_OK, response);
}
```

## **Converter 사용 규칙**

- Entity ↔ DTO 변환 로직은 반드시 Converter 클래스에서 처리한다.
- Service나 Controller에서 직접 변환하지 않는다.


## **Enum 위치 규칙**

- Enum은 해당 도메인의 enums/ 폴더에 모아둔다.

## **설정 클래스 분리 규칙**

- @EnableJpaAuditing, @EntityScan 등 JPA 관련 설정은 JpaConfig 클래스로 분리한다
- Application 클래스에 직접 붙이지 않는다.

## 인증 (MVP: 세션)

- Redis 세션 저장
- 권한 체크: Spring Security
- 역할: USER, BANK_ADMIN, DEV_ADMIN

## 주요 비즈니스 규칙

- SHAP 설명은 고객용/은행원용 반드시 분리 — 고객에게 내부 파생 변수 노출 금지
- LLM(Gemini, 추후 확정)으로 SHAP 설명을 고객 친화적 자연어로 변환
- KYC 인증 완료 후에만 대출 신청 가능
- Spring Batch만 AI 서버 호출 가능 (Controller에서 AI 직접 호출 금지)
- 동일 상품 중복 신청 불가
- 이미 결정된 심사 건 재처리 불가
- My Biz Data 미존재 시 대출 신청 불가

## ORM

- JPA 사용
- QueryDSL은 복잡한 동적 쿼리 필요 시 추후 추가
- N+1 문제 주의: fetch join 또는 @EntityGraph 사용

## AOP

- API 로그 수집은 admin-backend의 Spring AOP로 처리 (api_logs 테이블 저장)
- 로그 항목: 요청 경로, HTTP 메서드, 상태 코드, 처리 시간