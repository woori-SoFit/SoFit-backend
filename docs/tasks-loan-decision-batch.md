# 대출 심사 결정 배치 작업 계획

## 개요
S등급 산출 완료(`S_COMPLETED`) 상태의 대출 신청 건에 대해, `loan_rate_policy` 테이블 기반으로 자동 심사(SYSTEM_APPROVED / SYSTEM_REJECTED)를 수행하는 Spring Batch Job 구현

## 브랜치/커밋 정보
- 브랜치: `feat/SOFIT-XXX-loan-decision-batch`
- 커밋: `[SOFIT-XXX] Feat: 대출 금리 정책 기반 자동 심사 배치 구현`

---

## Phase 1: LoanRatePolicy 엔티티 및 Repository 생성

### 작업 내용
1. `sofit-common` > `entity/loan/LoanRatePolicy.java` — 엔티티 생성 (BaseEntity 상속)
2. `sofit-common` > `repository/LoanRatePolicyRepository.java` — Repository 생성
   - `findByProduct_ProductIdAndMinScoreLessThanEqualAndMaxScoreGreaterThan(Long productId, Integer scbGrade, Integer scbGrade)`

---

## Phase 2: Spring Batch Job 구현 (sofit-admin)

### 작업 내용
1. `sofit-admin` > `global/batch/LoanDecisionBatchConfig.java` — Batch Job/Step 설정
   - Job: `loanDecisionJob`
   - Step: `loanDecisionStep` (Tasklet 방식)
2. `sofit-admin` > `global/batch/LoanDecisionTasklet.java` — 비즈니스 로직
   - `S_COMPLETED` 상태 LoanApplication 조회
   - application_id로 Scb 테이블에서 scb_grade 조회
   - product_id + scb_grade로 LoanRatePolicy 매칭
   - 매칭 성공: status → SYSTEM_APPROVED, LoanDecision(APPROVED) 생성
   - 매칭 실패: status → SYSTEM_REJECTED, LoanDecision(REJECTED) 생성

---

## Phase 3: LoanApplicationRepository 쿼리 추가 및 LoanDecision 팩토리 메서드 보완

### 작업 내용
1. `LoanApplicationRepository`에 `findByStatus(ApplicationStatus status)` 추가
2. `LoanDecision`에 시스템 승인/거절용 팩토리 메서드 추가 (rejectionReason 필드)

---

## 처리 흐름 요약
```
[S_COMPLETED 신청 건 조회]
    ↓
[SCB 테이블에서 scb_grade 조회 (application_id 기반)]
    ↓
[loan_rate_policy에서 product_id + min_score ≤ scb_grade < max_score 매칭]
    ↓
├── 매칭 성공:
│   ├── approved_amount = min(requested_amount, max_limit)
│   ├── approved_rate = interest_rate
│   ├── approved_term = requested_term
│   ├── repayment_method = 신청 건의 repayment_method
│   ├── loan_application.status → SYSTEM_APPROVED
│   └── loan_decision 생성 (decision=APPROVED)
│
└── 매칭 실패:
    ├── loan_application.status → SYSTEM_REJECTED
    └── loan_decision 생성 (decision=REJECTED, rejection_reason="SCB 최소 등급 미달")
```
