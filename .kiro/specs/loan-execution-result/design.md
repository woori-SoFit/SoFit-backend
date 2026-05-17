# SOFIT-40 대출 실행 결과 조회 API — Design

## API Overview

| # | 기능 | Method | Path |
|---|------|--------|------|
| 1 | 대출 실행 결과 조회 | GET | /api/loan-applications/{applicationId}/execution |

---

## 응답 필드 소스 매핑

| 응답 필드 | 소스 테이블 | 소스 컬럼 | 비고 |
|-----------|------------|-----------|------|
| executionId | loan_execution | execution_id | PK |
| applicationId | loan_application | application_id | PK |
| productId | loan_product | product_id | loan_application.product_id FK 조인 |
| productName | loan_product | product_name | loan_application.product_id FK 조인 |
| executedAmount | loan_execution | execution_amount | 실제 실행액 (≤ 승인 한도) |
| approvedRate | loan_decision | approved_rate | decision=APPROVED 인 행 |
| approvedTerm | loan_decision | approved_term | decision=APPROVED 인 행 |
| repaymentMethod | loan_application | repayment_method | ENUM 문자열 그대로 반환 |

> ⚠️ `loan_decision.approved_amount`와 `loan_execution.execution_amount`는 다를 수 있음.
> 승인 한도(approved_amount) ≥ 실제 실행액(execution_amount). 응답의 `executedAmount`는 실제 실행값 사용.

---

## 조회 전략

```
loan_execution (기준)
  └── loan_application (execution.application_id = application.application_id)
        ├── loan_product (application.product_id = product.product_id)
        └── loan_decision (decision.application_id = application.application_id, decision = 'APPROVED')
```

- **JPA fetch join** 사용하여 단일 쿼리로 조회
- loan_execution을 기준 엔티티로 하여 application → product, decision 조인
- N+1 방지: `@Query` JPQL fetch join 또는 `@EntityGraph`

---

## 패키지 구조 (sofit-user 모듈)

```
sofit-user/src/main/java/com/sofit/user/domain/loan/
├── controller/
│   ├── LoanExecutionController.java
│   └── LoanExecutionControllerDocs.java
├── converter/
│   └── LoanExecutionConverter.java
├── dto/response/
│   └── LoanExecutionResultResponse.java
├── exception/
│   └── LoanErrorCode.java              ← 기존 파일에 EXECUTION_NOT_FOUND 추가
│   └── LoanSuccessCode.java            ← 기존 파일에 LOAN_EXECUTION_RESULT_OK 추가
├── repository/
│   └── LoanExecutionRepository.java    ← 신규
└── service/
    ├── LoanExecutionService.java       ← 신규 (interface)
    └── LoanExecutionServiceImpl.java   ← 신규
```

### 엔티티 (sofit-common 모듈 — 신규 생성)

```
sofit-common/src/main/java/com/sofit/common/entity/loan/
├── LoanExecution.java     ← 신규
└── LoanDecision.java      ← 신규
```

---

## 클래스 상세 설계

### 1. LoanExecution (Entity, sofit-common)

```java
@Entity
@Table(name = "loan_execution")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "execution_id")
    private Long executionId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private LoanApplication application;

    @Column(name = "execution_amount", nullable = false)
    private Long executionAmount;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "bank_code")
    private String bankCode;
}
```

### 2. LoanDecision (Entity, sofit-common)

```java
@Entity
@Table(name = "loan_decision")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoanDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "decision_id")
    private Long decisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false)
    private Decision decision;

    @Column(name = "approved_amount")
    private Long approvedAmount;

    @Column(name = "approved_rate")
    private java.math.BigDecimal approvedRate;

    @Column(name = "approved_term")
    private Integer approvedTerm;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    public enum Decision {
        APPROVED, REJECTED
    }
}
```

### 3. LoanExecutionRepository (sofit-user)

```java
public interface LoanExecutionRepository extends JpaRepository<LoanExecution, Long> {

    @Query("SELECT e FROM LoanExecution e " +
           "JOIN FETCH e.application a " +
           "JOIN FETCH a.product " +
           "WHERE a.applicationId = :applicationId " +
           "AND a.user.id = :userId " +
           "AND a.status = 'EXECUTED'")
    Optional<LoanExecution> findByApplicationIdAndUserId(
        @Param("applicationId") Long applicationId,
        @Param("userId") Long userId
    );
}
```

### 4. LoanExecutionResultResponse (record)

```java
public record LoanExecutionResultResponse(
    Long executionId,
    Long applicationId,
    Long productId,
    String productName,
    Long executedAmount,
    BigDecimal approvedRate,
    Integer approvedTerm,
    String repaymentMethod
) {}
```

### 5. LoanExecutionConverter

```java
public class LoanExecutionConverter {

    public static LoanExecutionResultResponse toResponse(
            LoanExecution execution, LoanDecision decision) {
        LoanApplication app = execution.getApplication();
        LoanProduct product = app.getProduct();

        return new LoanExecutionResultResponse(
            execution.getExecutionId(),
            app.getApplicationId(),
            product.getProductId(),
            product.getProductName(),
            execution.getExecutionAmount(),
            decision.getApprovedRate(),
            decision.getApprovedTerm(),
            app.getRepaymentMethod().name()
        );
    }
}
```

### 6. LoanExecutionService (interface)

```java
public interface LoanExecutionService {
    LoanExecutionResultResponse findExecutionResult(Long userId, Long applicationId);
}
```

### 7. LoanExecutionServiceImpl

```java
@Service
@RequiredArgsConstructor
public class LoanExecutionServiceImpl implements LoanExecutionService {

    private final LoanExecutionRepository loanExecutionRepository;
    private final LoanDecisionRepository loanDecisionRepository;

    @Override
    @Transactional(readOnly = true)
    public LoanExecutionResultResponse findExecutionResult(Long userId, Long applicationId) {
        // 1. loan_execution 조회 (application, product fetch join)
        LoanExecution execution = loanExecutionRepository
            .findByApplicationIdAndUserId(applicationId, userId)
            .orElseThrow(() -> new BaseException(LoanErrorCode.EXECUTION_NOT_FOUND));

        // 2. loan_decision (APPROVED) 조회
        LoanDecision decision = loanDecisionRepository
            .findByApplicationIdAndDecision(applicationId, LoanDecision.Decision.APPROVED)
            .orElseThrow(() -> new BaseException(LoanErrorCode.EXECUTION_NOT_FOUND));

        // 3. 변환
        return LoanExecutionConverter.toResponse(execution, decision);
    }
}
```

### 8. LoanExecutionController

```java
@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanExecutionController implements LoanExecutionControllerDocs {

    private static final Long TEMP_USER_ID = 1L; // TODO: 세션 인증 후 교체

    private final LoanExecutionService loanExecutionService;

    @GetMapping("/{applicationId}/execution")
    public ApiResponse<LoanExecutionResultResponse> getExecutionResult(
            @PathVariable Long applicationId) {
        LoanExecutionResultResponse response =
            loanExecutionService.findExecutionResult(TEMP_USER_ID, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_EXECUTION_RESULT_OK, response);
    }
}
```

### 9. LoanExecutionControllerDocs (Swagger 분리)

```java
public interface LoanExecutionControllerDocs {

    @Operation(summary = "대출 실행 결과 조회", description = "EXECUTED 상태인 대출 건의 실행 결과를 조회합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "실행 건을 찾을 수 없습니다.")
    })
    ApiResponse<LoanExecutionResultResponse> getExecutionResult(
        @Parameter(description = "대출 신청 ID") @PathVariable Long applicationId
    );
}
```

---

## Error Code 추가

| Enum 상수 | HTTP | code | message |
|-----------|------|------|---------|
| EXECUTION_NOT_FOUND | 404 | LOAN4043 | 실행 건을 찾을 수 없습니다. |

## Success Code 추가

| Enum 상수 | HTTP | code | message |
|-----------|------|------|---------|
| LOAN_EXECUTION_RESULT_OK | 200 | LOAN2008 | 대출 실행 결과 조회에 성공했습니다. |

---

## LoanDecisionRepository (sofit-user, 신규)

```java
public interface LoanDecisionRepository extends JpaRepository<LoanDecision, Long> {

    Optional<LoanDecision> findByApplication_ApplicationIdAndDecision(
        Long applicationId, LoanDecision.Decision decision
    );
}
```

---

## 시퀀스 다이어그램

```
Client → LoanExecutionController.getExecutionResult(applicationId)
  → LoanExecutionServiceImpl.findExecutionResult(TEMP_USER_ID, applicationId)
    → LoanExecutionRepository.findByApplicationIdAndUserId(applicationId, userId)
      ← LoanExecution (with application, product fetched)
    → LoanDecisionRepository.findByApplication_ApplicationIdAndDecision(applicationId, APPROVED)
      ← LoanDecision
    → LoanExecutionConverter.toResponse(execution, decision)
      ← LoanExecutionResultResponse
  ← ApiResponse.onSuccess(LOAN_EXECUTION_RESULT_OK, response)
← 200 OK
```
