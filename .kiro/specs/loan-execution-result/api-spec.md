# SOFIT-40 API 명세

## 공통

- **모듈**: `sofit-user`
- **인증**: 임시 permitAll (TODO: 세션 인증 구현 후 USER 권한 체크)
- **응답 포맷**:
```json
{
  "isSuccess": true,
  "code": "LOAN2010",
  "message": "대출 실행 결과 조회에 성공했습니다.",
  "result": {}
}
```

---

## API. 대출 실행 결과 조회

### Request

```
GET /api/loan-applications/{applicationId}/execution
```

| 파라미터 | 타입 | 위치 | 설명 |
|----------|------|------|------|
| applicationId | Long | Path Variable | 대출 신청 고유 ID |

### Response (200)

```json
{
  "isSuccess": true,
  "code": "LOAN2010",
  "message": "대출 실행 결과 조회에 성공했습니다.",
  "result": {
    "executionId": 1,
    "applicationId": 10,
    "productId": 3,
    "productName": "우리 소상공인 안심대출",
    "executedAmount": 100000000,
    "approvedRate": 4.32,
    "approvedTerm": 60,
    "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST"
  }
}
```

### Response 필드

| 필드명 | 타입 | 설명 |
|--------|------|------|
| executionId | Long | 대출 실행 고유 ID |
| applicationId | Long | 대출 신청 고유 ID |
| productId | Long | 대출 상품 고유 ID |
| productName | String | 대출 상품명 |
| executedAmount | Long | 실제 실행 금액 (원) |
| approvedRate | BigDecimal | 확정 금리 (%) |
| approvedTerm | Integer | 확정 대출 기간 (개월) |
| repaymentMethod | String | 상환 방식 (EQUAL_PRINCIPAL_INTEREST / EQUAL_PRINCIPAL / BALLOON) |

---

## Error Response

| 상황 | HTTP | code | message |
|------|------|------|---------|
| applicationId 미존재 | 404 | LOAN4044 | 실행 건을 찾을 수 없습니다. |
| 본인 소유 아님 | 404 | LOAN4044 | 실행 건을 찾을 수 없습니다. |
| status ≠ EXECUTED | 404 | LOAN4044 | 실행 건을 찾을 수 없습니다. |
| loan_execution row 없음 | 404 | LOAN4044 | 실행 건을 찾을 수 없습니다. |
| loan_decision row 없음 (정합성 깨짐) | 404 | LOAN4043 | 심사 결정 정보를 찾을 수 없습니다. |

> 사용자 입력 관련 실패 4종은 LOAN4044로 통합 (정보 노출 방지). loan_decision 누락은 데이터 정합성 문제로 별도 코드(LOAN4043, SOFIT-34 정의 재사용).

### Error Response 예시

```json
{
  "isSuccess": false,
  "code": "LOAN4044",
  "message": "실행 건을 찾을 수 없습니다.",
  "result": null
}
```

---

## Apidog 테스트

### 성공 케이스

```
GET http://localhost:8080/api/loan-applications/10/execution
```

> 사전 조건: loan_application(id=10, user_id=1, status=EXECUTED) + loan_execution(application_id=10) + loan_decision(application_id=10, decision=APPROVED) 데이터 존재

### 실패 케이스 — 존재하지 않는 ID

```
GET http://localhost:8080/api/loan-applications/99999/execution
```

### 실패 케이스 — status가 EXECUTED가 아닌 건

```
GET http://localhost:8080/api/loan-applications/1/execution
```

> loan_application(id=1)의 status가 SUBMITTED 등인 경우

---

## 테스트 데이터 (Apidog용 INSERT)

```sql
-- loan_product (이미 존재한다면 생략)
INSERT INTO loan_product (product_id, product_name, description, status, min_rate, max_rate, min_limit, max_limit, min_term, max_term, title, subtitle, created_by)
VALUES (3, '우리 소상공인 안심대출', '소상공인 전용 대출', 'ACTIVE', 3.50, 7.00, 10000000, 500000000, 12, 120, '안심 대출', '소상공인 맞춤', 1);

-- loan_application (status = EXECUTED)
INSERT INTO loan_application (application_id, user_id, product_id, requested_amount, requested_term, repayment_method, status, applied_at, updated_at)
VALUES (10, 1, 3, 100000000, 60, 'EQUAL_PRINCIPAL_INTEREST', 'EXECUTED', NOW(), NOW());

-- loan_decision (APPROVED)
INSERT INTO loan_decision (application_id, decision, approved_amount, approved_rate, approved_term)
VALUES (10, 'APPROVED', 150000000, 4.32, 60);

-- loan_execution
INSERT INTO loan_execution (application_id, execution_amount, account_number, bank_code)
VALUES (10, 100000000, '1002123456789', '020');
```

> 인증 없이 호출 가능 (임시 permitAll 상태)
