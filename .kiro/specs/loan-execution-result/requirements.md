# SOFIT-40 대출 실행 결과 조회 API — Requirements

## 개요

대출 실행(EXECUTED) 완료된 건에 대해 고객(USER)이 실행 결과를 조회하는 API.

---

## REQ-1. 대출 실행 결과 단건 조회

**User Story**: 대출이 실행된 고객으로서, 실행 결과(실행 금액, 확정 금리, 기간, 상환 방식 등)를 확인하고 싶다.

### Acceptance Criteria

| # | 조건 | 기대 결과 |
|---|------|-----------|
| AC-1 | `GET /api/loan-applications/{applicationId}/execution` 호출 시 해당 건이 본인 소유이고 status=EXECUTED이며 loan_execution row가 존재 | 200 + `LOAN2010` 응답, result에 실행 결과 필드 포함 |
| AC-2 | applicationId가 존재하지 않음 | 404 + `LOAN4044` |
| AC-3 | applicationId가 존재하지만 본인 소유가 아님 | 404 + `LOAN4044` (정보 노출 방지) |
| AC-4 | applicationId가 본인 소유이지만 status ≠ EXECUTED | 404 + `LOAN4044` |
| AC-5 | applicationId가 본인 소유이고 status=EXECUTED이지만 loan_execution row 없음 | 404 + `LOAN4044` |
| AC-6 | EXECUTED 상태인데 loan_decision row 없음 (데이터 정합성 깨짐) | 404 + `LOAN4043` (LOAN_DECISION_NOT_FOUND) |

---

## REQ-2. 응답 필드 정확성

**User Story**: 고객으로서, 실행 결과에 정확한 실행 금액·확정 금리·기간·상환 방식이 표시되길 원한다.

### Acceptance Criteria

| # | 조건 | 기대 결과 |
|---|------|-----------|
| AC-1 | executedAmount | loan_execution.execution_amount 값 반환 (승인 한도와 다를 수 있음) |
| AC-2 | approvedRate | loan_decision(decision=APPROVED).approved_rate 값 반환 |
| AC-3 | approvedTerm | loan_decision(decision=APPROVED).approved_term 값 반환 |
| AC-4 | repaymentMethod | loan_application.repayment_method 값 반환 |
| AC-5 | productName | loan_product.product_name 값 반환 (loan_application.product_id 조인) |
| AC-6 | executionId | loan_execution.execution_id 값 반환 |
| AC-7 | applicationId | loan_application.application_id 값 반환 |
| AC-8 | productId | loan_product.product_id 값 반환 |

---

## REQ-3. 인증 정책 (임시)

**User Story**: 개발 단계에서 인증 없이 API를 테스트할 수 있어야 한다.

### Acceptance Criteria

| # | 조건 | 기대 결과 |
|---|------|-----------|
| AC-1 | 인증 헤더 없이 호출 | permitAll 정책으로 정상 응답 |
| AC-2 | userId | 하드코딩 `1L` 사용 (TODO: 세션 인증 구현 후 교체) |

---

## REQ-4. 조회 성능

**User Story**: 조회 시 N+1 문제 없이 단일 쿼리로 결과를 가져와야 한다.

### Acceptance Criteria

| # | 조건 | 기대 결과 |
|---|------|-----------|
| AC-1 | loan_execution → loan_application → loan_product, loan_application → loan_decision 조인 | fetch join 또는 @EntityGraph로 N+1 방지 |
