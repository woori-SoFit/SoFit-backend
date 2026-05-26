# 금융인증서 서비스 분리 (FinancialCertService)

## 배경

`AuthServiceImpl.verifyFinancialCertificate()`가 **외부 API 인증 + 회원가입 후처리**를 한 메서드에서 모두 처리하고 있어, 마이 비즈니스 데이터 / 대출신청 등 다른 플로우에서 재사용이 어려운 구조였음.

---

## 현재 구조의 문제점

```java
// AuthServiceImpl
public FinancialCertVerifyResponse verifyFinancialCertificate(
        FinancialCertVerifyRequest request, HttpSession session) {

    // 1. 외부 API 호출 + 검증
    ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
            externalMockClient.callFinancialCertVerify(...);

    // 2. 회원가입 후처리 — 다른 플로우에서 쓰면 불필요하거나 충돌 가능
    Long processId = (Long) session.getAttribute(REGISTRATIONPROCESSID);
    if (processId != null) {
        processRegistrationStep2(processId);
    }
}
```

- 인증 로직과 회원가입 후처리가 **한 메서드에 혼재**
- 다른 플로우에서 호출 시 **세션까지 함께 넘겨야 하는 불필요한 의존성** 발생
- `ExternalMockClient`가 `AuthServiceImpl`에 직접 묶여 있어 **재사용 및 교체 어려움**

---

## 변경 내용

### 1. `FinancialCertService` 신규 생성

외부 API 호출과 응답 검증만 담당하는 순수 서비스.  
세션, HTTP, Spring Security 등 웹 레이어 의존성 없음.

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialCertService {

    private final ExternalMockClient externalMockClient;

    public ExternalFinancialCertResponse verify(FinancialCertVerifyRequest request) {

        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                externalMockClient.callFinancialCertVerify(
                        request.getPhoneNumber(), request.getPin());

        if (!mockResponse.isSuccess()) {
            String code = mockResponse.code();
            if ("AUTH4001".equals(code)) {
                throw new BaseException(AuthErrorCode.PIN_MISMATCH);
            }
            throw new BaseException(AuthErrorCode.CERT_NOT_FOUND);
        }

        ExternalFinancialCertResponse certResult = mockResponse.result();
        if (!"VALID".equals(certResult.status())) {
            throw new BaseException(AuthErrorCode.CERT_VERIFICATION_FAILED);
        }

        return certResult;
    }
}
```

### 2. `AuthServiceImpl` 수정

인증은 `FinancialCertService`에 위임하고, 회원가입 후처리만 담당.

```java
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final FinancialCertService financialCertService; // 주입 추가

    @Override
    public FinancialCertVerifyResponse verifyFinancialCertificate(
            FinancialCertVerifyRequest request, HttpSession session) {

        // 인증은 Core에 위임
        ExternalFinancialCertResponse certResult = financialCertService.verify(request);

        // 회원가입 후처리만 여기서 담당
        Long processId = (Long) session.getAttribute(REGISTRATIONPROCESSID);
        if (processId != null) {
            processRegistrationStep2(processId);
        }

        return AuthConverter.toFinancialCertVerifyResponse(certResult);
    }
}
```

### 3. 다른 플로우에서 재사용

`FinancialCertService`만 주입하면 됨. **세션 불필요**.

```java
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private final FinancialCertService financialCertService;

    public void updateBusinessData(FinancialCertVerifyRequest request, Long userId) {

        // 동일한 인증 재사용
        ExternalFinancialCertResponse certResult = financialCertService.verify(request);

        // 마이 비즈니스 후처리
        // ...
    }
}
```

---

## 구조 비교

### 변경 전

```
AuthServiceImpl.verifyFinancialCertificate(request, session)
└── ExternalMockClient 직접 호출
└── 회원가입 후처리 (세션 의존)
```

### 변경 후

```
FinancialCertService.verify(request)   ← 인증만, 재사용 가능
        ↑                        ↑
AuthServiceImpl            BusinessServiceImpl
verifyFinancialCertificate  updateBusinessData
(회원가입 후처리)            (비즈니스 후처리)
세션 O                       세션 X
```

---

## 핵심 포인트

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| 인증 책임 | `AuthServiceImpl`에 혼재 | `FinancialCertService`로 분리 |
| 세션 의존성 | Core까지 세션이 흘러들어옴 | 회원가입 후처리 레이어에서만 사용 |
| 재사용성 | 회원가입 플로우에만 종속 | 어느 플로우에서든 주입해서 사용 가능 |
| ExternalMockClient 관리 | `AuthServiceImpl`에 직접 묶임 | `FinancialCertService` 한 곳에서만 관리 |
| 실제 API 교체 시 | 여러 곳 수정 필요 | `FinancialCertService`만 수정 |
