# 대출 심사 승인/거절 API 역할 분기 리팩토링

## 개요
- 승인/거절 API에서 역할(ADMIN_BANK_TELLER vs ADMIN_BANK_MANAGER)에 따라 동작을 분기
- `createdBy`를 `SecurityUtil`에서 가져온 userId로 명시적 설정
- 브랜치: `feat/SOFIT-{번호}-loan-decision-role-branch`
- 커밋: `[SOFIT-{번호}] Refactor: 대출 심사 승인/거절 API 역할 분기 처리`

---

## Phase 1: BaseEntity에 createdBy setter 추가 + LoanDecision 팩토리 메서드 수정

### 변경 파일
1. `sofit-common/.../entity/BaseEntity.java` — `setCreatedBy(Long)` 메서드 추가
2. `sofit-common/.../entity/loan/LoanDecision.java` — `createApproval`, `createRejection`에 `createdBy` 파라미터 추가

---

## Phase 2: LoanDecisionServiceImpl 로직 수정

### 변경 파일
1. `sofit-admin/.../loan/service/LoanDecisionServiceImpl.java`

### 승인 (approveLoanApplication) 변경사항
- `AdminRoleService` 주입 추가
- `SecurityUtil.getCurrentUserId()`로 userId 획득 → `LoanDecision` 생성 시 전달
- 역할 분기:
  - `ADMIN_BANK_TELLER`: status → `MANAGER_REVIEW`, 알림 생성 안 함
  - `ADMIN_BANK_MANAGER`: status → `APPROVED`, 알림 생성 (기존 로직)

### 거절 (rejectLoanApplication) 변경사항
- `SecurityUtil.getCurrentUserId()`로 userId 획득 → `LoanDecision` 생성 시 전달
- 역할 분기:
  - `ADMIN_BANK_TELLER`: status → `MANAGER_REVIEW`, 알림 생성 안 함
  - `ADMIN_BANK_MANAGER`: status → `REJECTED`, 알림 생성 (기존 로직)

---

## Phase 3: 기존 validateDecisionAuthority 검증 로직 수정

### 변경 파일
1. `sofit-admin/.../loan/service/LoanDecisionServiceImpl.java`

### 변경사항
- 기존: `SYSTEM_APPROVED → TELLER만`, `MANAGER_REVIEW → MANAGER만` 으로 엄격 분기
- 변경: 새 요구사항에 맞게 조정 (TELLER는 SYSTEM_APPROVED 상태에서, MANAGER는 MANAGER_REVIEW 상태에서 처리 가능 — 기존과 동일하므로 유지)

---

## 동작 요약

| API | 역할 | loan_application.status 변경 | loan_decision 저장 | notification 저장 |
|-----|------|-----|-----|-----|
| approve | ADMIN_BANK_TELLER | → MANAGER_REVIEW | ✅ (APPROVED, createdBy=userId) | ❌ |
| approve | ADMIN_BANK_MANAGER | → APPROVED | ✅ (APPROVED, createdBy=userId) | ✅ |
| reject | ADMIN_BANK_TELLER | → MANAGER_REVIEW | ✅ (REJECTED, createdBy=userId) | ❌ |
| reject | ADMIN_BANK_MANAGER | → REJECTED | ✅ (REJECTED, createdBy=userId) | ✅ |
