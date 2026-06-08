# Decision Enum 분기 리팩토링

## 개요
`Decision` enum을 `APPROVED`/`REJECTED` 2개에서 단계별 6개(`SYSTEM_APPROVED`, `SYSTEM_REJECTED`, `TELLER_APPROVED`, `TELLER_REJECTED`, `MANAGER_APPROVED`, `MANAGER_REJECTED`)로 분기하여, `createdBy == null` 판별 패턴 제거 + "가장 최근 건 조회" 의존 제거

## 브랜치 / 커밋 정보
- 브랜치: `feat/SOFIT-108-loan-decision-enum-refactor`
- 커밋: `[SOFIT-108] Refactor: Decision enum 단계별 분기 리팩토링`

---

## Phase 1: Decision enum 변경 + LoanDecision 엔티티 수정

### 변경 파일
- `sofit-common/.../entity/loan/enums/Decision.java`
- `sofit-common/.../entity/loan/LoanDecision.java`

### 작업 내용
1. `Decision` enum 값 변경:
   ```java
   public enum Decision {
       SYSTEM_APPROVED,
       SYSTEM_REJECTED,
       TELLER_APPROVED,
       TELLER_REJECTED,
       MANAGER_APPROVED,
       MANAGER_REJECTED;

       public boolean isFinal() {
           return this != SYSTEM_APPROVED && this != SYSTEM_REJECTED;
       }

       public boolean isApproved() {
           return this == SYSTEM_APPROVED || this == TELLER_APPROVED || this == MANAGER_APPROVED;
       }

       public boolean isRejected() {
           return this == SYSTEM_REJECTED || this == TELLER_REJECTED || this == MANAGER_REJECTED;
       }
   }
   ```

2. `LoanDecision` 팩토리 메서드 수정:
   - `createSystemApproval()` → `decision = Decision.SYSTEM_APPROVED`
   - `createSystemRejection()` → `decision = Decision.SYSTEM_REJECTED`
   - `createApproval()` → 파라미터에 `Decision decision` 추가 (TELLER_APPROVED 또는 MANAGER_APPROVED를 호출부에서 전달)
   - `createRejection()` → 파라미터에 `Decision decision` 추가 (TELLER_REJECTED 또는 MANAGER_REJECTED를 호출부에서 전달)

---

## Phase 2: LoanDecisionRepository 쿼리 메서드 추가

### 변경 파일
- `sofit-common/.../repository/LoanDecisionRepository.java`

### 작업 내용
1. 최종 결정 조회 메서드 추가:
   ```java
   Optional<LoanDecision> findByApplication_ApplicationIdAndDecisionIn(
       Long applicationId, List<Decision> decisions);
   ```

2. 시스템 심사 조회 메서드 추가:
   ```java
   Optional<LoanDecision> findByApplication_ApplicationIdAndDecision(
       Long applicationId, Decision decision);
   ```

3. 기존 `findTopByApplication_ApplicationIdOrderByCreatedAtDesc`는 당장 삭제하지 않고 deprecated 처리 (하위 호환)

---

## Phase 3: admin-backend 서비스 수정

### 변경 파일
- `sofit-admin/.../loan/service/LoanDecisionServiceImpl.java`
- `sofit-admin/.../loan/converter/LoanApplicationReviewConverter.java`
- `sofit-admin/.../loan/service/LoanApplicationReviewServiceImpl.java`
- `sofit-admin/.../global/batch/LoanDecisionProcessor.java`

### 작업 내용

#### LoanDecisionServiceImpl
- `approveLoanApplication()`:
  - TELLER → `LoanDecision.createApproval(..., Decision.TELLER_APPROVED)`
  - MANAGER → `LoanDecision.createApproval(..., Decision.MANAGER_APPROVED)`
- `rejectLoanApplication()`:
  - TELLER → `LoanDecision.createRejection(..., Decision.TELLER_REJECTED)`
  - MANAGER → `LoanDecision.createRejection(..., Decision.MANAGER_REJECTED)`

#### LoanApplicationReviewServiceImpl
- 시스템 심사 추출 로직 변경:
  ```java
  // Before
  decisions.stream()
      .filter(d -> d.getCreatedBy() == null && d.getDecision() == Decision.APPROVED)
  
  // After
  decisions.stream()
      .filter(d -> d.getDecision() == Decision.SYSTEM_APPROVED)
  ```

#### LoanApplicationReviewConverter
- `toRecommendationResponse()`: `createdBy != null` 체크 제거 → `decision != Decision.SYSTEM_APPROVED` 체크로 변경
- `toDecisionResponse()`: `createdBy == null` 분기 제거 → `decision.getDecision().name()`으로 직접 status 매핑

#### LoanDecisionProcessor (배치)
- 이미 `createSystemApproval()`/`createSystemRejection()` 사용 중 → Phase 1에서 내부 값만 바뀌므로 추가 수정 불필요

---

## Phase 4: user-backend 서비스 수정

### 변경 파일
- `sofit-user/.../loan/service/LoanServiceImpl.java`
- `sofit-user/.../loan/service/LoanExecutionServiceImpl.java`
- `sofit-user/.../loan/converter/LoanConverter.java`

### 작업 내용

#### LoanServiceImpl - `findCompletedLoanDetail()`
```java
// Before: 가장 최근 건 조회
loanDecisionRepository.findTopByApplication_ApplicationIdOrderByCreatedAtDesc(applicationId)

// After: 최종 결정만 조회
loanDecisionRepository.findByApplication_ApplicationIdAndDecisionIn(
    applicationId,
    List.of(Decision.MANAGER_APPROVED, Decision.MANAGER_REJECTED, Decision.TELLER_REJECTED)
)
```

#### LoanExecutionServiceImpl
- `findExecutionResult()`: 같은 방식으로 최종 승인 건 조회 변경
  ```java
  loanDecisionRepository.findByApplication_ApplicationIdAndDecision(
      applicationId, Decision.MANAGER_APPROVED)
  ```
- `confirmAccountVerification()`: 동일하게 `MANAGER_APPROVED` 조회로 변경 (대출 실행은 최종 승인 건에서만 발생)

---

## Phase 5: DB 마이그레이션

### 작업 내용
기존 `loan_decision` 테이블의 `decision` 컬럼 데이터 변환:
```sql
-- 시스템 심사 (created_by IS NULL)
UPDATE loan_decision SET decision = 'SYSTEM_APPROVED' WHERE decision = 'APPROVED' AND created_by IS NULL;
UPDATE loan_decision SET decision = 'SYSTEM_REJECTED' WHERE decision = 'REJECTED' AND created_by IS NULL;

-- 은행원 심사 (created_by IS NOT NULL) - 행원/지점장 구분
-- → 행원: ApplicationStatus가 MANAGER_REVIEW로 전환된 시점의 decision
-- → 지점장: ApplicationStatus가 APPROVED/REJECTED로 전환된 시점의 decision
-- 정확한 구분이 어려우면 일괄 TELLER_APPROVED/TELLER_REJECTED로 마이그레이션 후 수동 보정
UPDATE loan_decision SET decision = 'TELLER_APPROVED' WHERE decision = 'APPROVED' AND created_by IS NOT NULL;
UPDATE loan_decision SET decision = 'TELLER_REJECTED' WHERE decision = 'REJECTED' AND created_by IS NOT NULL;
```

> 참고: 지점장 결정 건은 `loan_application.status`가 `APPROVED`/`REJECTED`인 건의 마지막 decision이므로 join으로 구분 가능하나, 데이터가 적으면 수동 보정이 더 안전

---

## 영향 범위 요약

| 모듈 | 파일 | 변경 유형 |
|------|------|-----------|
| common | `Decision.java` | enum 값 변경 |
| common | `LoanDecision.java` | 팩토리 메서드 시그니처 변경 |
| common | `LoanDecisionRepository.java` | 쿼리 메서드 추가 |
| admin | `LoanDecisionServiceImpl.java` | Decision 값 전달 로직 변경 |
| admin | `LoanApplicationReviewServiceImpl.java` | 필터 조건 변경 |
| admin | `LoanApplicationReviewConverter.java` | `createdBy` null 체크 → enum 기반 분기 |
| admin | `LoanDecisionProcessor.java` | 변경 없음 (팩토리 내부만 변경) |
| user | `LoanServiceImpl.java` | 최종 결정 조회 쿼리 변경 |
| user | `LoanExecutionServiceImpl.java` | 최종 승인 조회 쿼리 변경 |
| DB | `loan_decision` | decision 컬럼 데이터 마이그레이션 |
