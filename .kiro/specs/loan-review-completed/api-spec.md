# SOFIT-34 API 명세

> 심사 완료된 대출 목록/상세 조회 API (USER)
> 심사 결과 데이터는 신규 `loan_decision` 테이블에서 조회.

## 공통

- **모듈**: `sofit-user`
- **인증**: 임시 permitAll (`TEMP_USER_ID = 1L`, TODO: 세션 인증)
- **응답 코드**: `LoanSuccessCode` 도메인 코드 사용 (`LOAN2008`, `LOAN2009`) — SOFIT-28/32 컨벤션 통일
- **응답 포맷**:
```json
{
  "isSuccess": true,
  "code": "LOAN20XX",
  "message": "...",
  "result": {}
}
```

---

## 심사 완료 상태 범위

```
status IN (APPROVED, REJECTED)
```

> `loan_decision.decision` enum이 `APPROVED / REJECTED` 두 값이므로 범위 일치.
> `CONTRACTED`, `EXECUTED`, `CANCELLED`는 본 API 범위 외 (계약/실행/취소 별도 화면).

---

## API 1. 심사 완료 대출 목록 조회

### Request
```
GET /api/loan-applications/completed
```

### Response (200)
```json
{
  "isSuccess": true,
  "code": "LOAN2008",
  "message": "심사 완료 대출 목록 조회에 성공했습니다.",
  "result": {
    "loanApplications": [
      {
        "applicationId": 10002,
        "productName": "우리 사장님 대출",
        "status": "APPROVED",
        "requestedAmount": 70000000,
        "appliedAt": "2026-05-02",
        "updatedAt": "2026-05-07"
      }
    ]
  }
}
```

> `result`는 wrapper 객체 (`CompletedLoanListResponse`) — SOFIT-32 `LoanApplicationListResponse` 패턴 동일. 내부 `loanApplications` 필드에 `CompletedLoanItem` 배열.

### 필드

| 필드명 | 타입 | 설명 |
|---|---|---|
| applicationId | Long | 대출 신청 고유 ID |
| productName | String | 대출 상품명 |
| status | Enum (ApplicationStatus) | 심사 단계 (APPROVED / REJECTED) |
| requestedAmount | Long | 신청 금액 (원) |
| appliedAt | LocalDate | 신청 일자 (yyyy-MM-dd) |
| updatedAt | LocalDate | 심사 완료 일자 (yyyy-MM-dd) |

### 조회 조건
- `userId = 1` (임시 하드코딩)
- `status IN (APPROVED, REJECTED)`
- 정렬: `updatedAt DESC`

---

## API 2. 심사 완료 대출 상세 조회

### Request
```
GET /api/loan-applications/completed/{applicationId}
```

### Response (200) — 승인
```json
{
  "isSuccess": true,
  "code": "LOAN2009",
  "message": "심사 완료 대출 상세 조회에 성공했습니다.",
  "result": {
    "applicationId": 10002,
    "productName": "우리 사장님 대출",
    "requestedAmount": 70000000,
    "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST",
    "decisionInfo": {
      "decision": "APPROVED",
      "approvedAmount": 65000000,
      "approvedRate": 4.25,
      "approvedTerm": 60,
      "rejectionReason": null
    }
  }
}
```

### Response (200) — 거절
```json
{
  "isSuccess": true,
  "code": "LOAN2009",
  "message": "심사 완료 대출 상세 조회에 성공했습니다.",
  "result": {
    "applicationId": 10002,
    "productName": "우리 사장님 대출",
    "requestedAmount": 70000000,
    "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST",
    "decisionInfo": {
      "decision": "REJECTED",
      "approvedAmount": null,
      "approvedRate": null,
      "approvedTerm": null,
      "rejectionReason": "신용도 기준 미달"
    }
  }
}
```

### 필드

| 필드명 | 타입 | 설명 |
|---|---|---|
| applicationId | Long | 대출 신청 고유 ID |
| productName | String | 대출 상품명 |
| requestedAmount | Long | 신청 금액 (원) |
| repaymentMethod | Enum (RepaymentMethod) | 상환 방식 |
| decisionInfo | Object | 심사 결정 결과 (아래) |
| decisionInfo.decision | Enum (Decision) | APPROVED / REJECTED |
| decisionInfo.approvedAmount | Long | 승인 금액 (원) — 거절 시 null |
| decisionInfo.approvedRate | BigDecimal | 확정 금리 (%) — 거절 시 null |
| decisionInfo.approvedTerm | Integer | 대출 기간 (개월) — 거절 시 null |
| decisionInfo.rejectionReason | String | 거절 사유 — 승인 시 null |

> 상세 응답에는 `status`, `appliedAt`, `updatedAt`, `requestedTerm` 필드가 **없음** (목록과 다름). `requestedTerm`은 `decisionInfo.approvedTerm`으로 대체된 것으로 해석.

### Error Response

| 상황 | HTTP | code | message |
|---|---|---|---|
| 존재하지 않는 applicationId (또는 본인 소유 아님) | 404 | LOAN4042 | 존재하지 않는 대출 신청입니다. |
| 심사 완료 상태 아님 (status NOT IN (APPROVED, REJECTED)) | 404 | LOAN4042 | 존재하지 않는 대출 신청입니다. |
| `loan_decision` 레코드 없음 | 404 | LOAN4043 | 심사 결정 정보를 찾을 수 없습니다. |

---

## 테이블: loan_decision (신규)

| 컬럼 | 타입 | NULL | 설명 |
|---|---|---|---|
| decision_id | BIGINT | PK | 심사 결정 고유 ID |
| application_id | BIGINT | NOT NULL | FK → loan_application.application_id |
| decision | ENUM | NOT NULL | APPROVED / REJECTED |
| approved_amount | BIGINT | NULL | 승인 금액 (원) |
| approved_rate | DECIMAL(5,2) | NULL | 확정 금리 (%) |
| approved_term | INT | NULL | 확정 기간 (개월) |
| rejection_reason | TEXT | NULL | 거절 사유 |

> 1:1 관계 (LoanApplication ↔ LoanDecision). application_id에 unique 제약 권장.

---

## Apidog 테스트
```
GET http://localhost:8080/api/loan-applications/completed
GET http://localhost:8080/api/loan-applications/completed/{applicationId}
```
