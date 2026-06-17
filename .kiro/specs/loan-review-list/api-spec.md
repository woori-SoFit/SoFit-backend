# SOFIT-32 API 명세

## 공통

- **모듈**: `sofit-user`
- **인증**: 임시 permitAll (TODO: 세션 인증 구현 후 USER 권한 체크)
- **응답 포맷**:
```json
{
  "isSuccess": true,
  "code": "COMMON2000",
  "message": "성공입니다.",
  "result": {}
}
```

---

## API 1. 심사 중인 대출 목록 조회

### Request

```
GET /api/loan-applications
```

### Response (200)

```json
{
  "isSuccess": true,
  "code": "COMMON2000",
  "message": "성공입니다.",
  "result": [
    {
      "applicationId": 10001,
      "productName": "소상공인 성장 대출",
      "status": "CB_CHECKING",
      "requestedAmount": 100000000,
      "appliedAt": "2026-05-08"
    }
  ]
}
```

### Response 필드

| 필드명 | 타입 | 설명 |
|---|---|---|
| applicationId | Long | 대출 신청 고유 ID |
| productName | String | 상품명 |
| status | Enum (ApplicationStatus) | 심사 단계 |
| requestedAmount | Long | 신청 금액 (원) |
| appliedAt | LocalDate | 신청 일시 (날짜만) |

### 조회 조건

- `userId = 1` (임시 하드코딩, TODO: 세션에서 추출)
- `status IN (SUBMITTED, CB_CHECKING, BASIC_REVIEW, SCB_CALCULATING, FINAL_REVIEW)`

---

## API 2. 심사 중인 대출 상세 조회

### Request

```
GET /api/loan-applications/{applicationId}
```

| 파라미터 | 타입 | 설명 |
|---|---|---|
| applicationId | Long | 대출 신청 고유 ID (Path Variable) |

### Response (200)

```json
{
  "isSuccess": true,
  "code": "COMMON2000",
  "message": "성공입니다.",
  "result": {
    "applicationId": 10001,
    "productName": "소상공인 성장 대출",
    "status": "CB_CHECKING",
    "requestedAmount": 100000000,
    "requestedTerm": 60,
    "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST",
    "appliedAt": "2026-05-08T14:35:00"
  }
}
```

### Response 필드

| 필드명 | 타입 | 설명 |
|---|---|---|
| applicationId | Long | 대출 신청 고유 ID |
| productName | String | 대출 상품명 |
| status | Enum (ApplicationStatus) | 심사 단계 |
| requestedAmount | Long | 신청 금액 (원) |
| requestedTerm | Integer | 대출 기간 (개월) |
| repaymentMethod | Enum (RepaymentMethod) | 상환 방식 |
| appliedAt | LocalDateTime | 신청 일시 |

### Error Response

| 상황 | HTTP | code | message |
|---|---|---|---|
| 존재하지 않는 applicationId (또는 본인 소유 아님) | 404 | LOAN4041 | 대출 신청 건을 찾을 수 없습니다. |

---

## Apidog 테스트

```
GET http://localhost:8080/api/loan-applications
GET http://localhost:8080/api/loan-applications/{applicationId}
```

> 인증 없이 호출 가능 (임시 permitAll 상태)
