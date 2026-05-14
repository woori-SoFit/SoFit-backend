# Design

## API Overview

| # | 기능 | Method | Path |
|---|---|---|---|
| 1 | 대출 상품 목록 조회 | GET | /api/loan-products |
| 2 | 대출 상품 상세 조회 | GET | /api/loan-products/{productId} |
| 3 | 신청 가능 여부 확인 | POST | /api/loan-products/{productId}/check |
| 4 | 대출 심사 요청 | POST | /api/loan-applications/{applicationId}/submit |

### 공통 응답 형식
```json
{
  "isSuccess": true,
  "code": "LOAN200X",
  "message": "string",
  "result": {}
}
```

---

## API 상세

### 1. 대출 상품 목록 조회
**GET** `/api/loan-products`

**Response (200, code: LOAN2001):**
```json
{
  "isSuccess": true,
  "code": "LOAN2001",
  "message": "대출 상품 목록 조회에 성공했습니다.",
  "result": {
    "loan_products": [
      { "productId": 1, "productName": "우리 사장님 대출", "title": "우리 사장님 곁을 든든하게!" },
      { "productId": 2, "productName": "우리카드 가맹점 우대 대출", "title": "매출대금 입금 중인 개인사업자 전용 대출" },
      { "productId": 3, "productName": "우리은행 네이버 스마트스토어 대출", "title": "네이버 스마트스토어 개인사업자 전용 대출" },
      { "productId": 4, "productName": "우리 Oh!(5)클릭 대출", "title": "개인사업자 전용 비대면 대출상품" }
    ]
  }
}
```

---

### 2. 대출 상품 상세 조회
**GET** `/api/loan-products/{productId}`

**Response (200, code: LOAN2002):**
```json
{
  "isSuccess": true,
  "code": "LOAN2002",
  "message": "대출 상품 상세 조회에 성공했습니다.",
  "result": {
    "productId": 4,
    "productName": "우리 Oh!(5)클릭 대출",
    "title": "빠르고 간편한 사업자 대출",
    "subtitle": "개인사업자 신속 · 초단편 비대면 대출상품",
    "maxLimit": "30,000,000",
    "maxTerm": "5",
    "industryType": "개인사업자",
    "interest_rate": {
      "minRate": 3.2,
      "maxRate": 6.0
    }
  }
}
```

---

### 3. 신청 가능 여부 확인
**POST** `/api/loan-products/{productId}/check`

**Request Header:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "user_input_annual_income": "AMT_0_30M",
  "user_input_credit_score": "CS_0_850",
  "user_input_income_type": "01",
  "user_input_existing_loan_amt": "LOAN_100M_OVER"
}
```

**Request Body 필드 정의:**

| 필드명 | 타입 | 허용값 | 설명 |
|---|---|---|---|
| user_input_annual_income | ENUM | AMT_0_30M, AMT_30_50M, AMT_50_100M, AMT_100M_OVER | 고객 입력 연소득 |
| user_input_credit_score | ENUM | CS_0_850, CS_850_OVER, CS_UNKNOWN | 고객 입력 신용점수 |
| user_input_income_type | ENUM | 01, 02, 03 | 소득 종류 (01:급여, 02:사업, 03:기타) |
| user_input_existing_loan_amt | ENUM | LOAN_100M_OVER, LOAN_0_100M, LOAN_NONE | 고객 기보유 대출액 |

**Response (200) - 신청 가능:**
```json
{
  "isSuccess": true,
  "code": "LOAN2003",
  "message": "신청 가능 여부 확인에 성공했습니다.",
  "result": {
    "eligible": true,
    "application_id": 1
  }
}
```

**Response (200) - 신청 불가:**
```json
{
  "isSuccess": true,
  "code": "LOAN2004",
  "message": "대출 신청 조건에 부합하지 않습니다.",
  "result": {
    "eligible": false,
    "application_id": 1
  }
}
```

---

### 4. 대출 심사 요청
**POST** `/api/loan-applications/{applicationId}/submit`

**Request Header:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "requestedAmount": 30000000,
  "requestedTerm": 60,
  "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST",
  "purpose": "WORKING_CAPITAL"
}
```

**Response (200, code: LOAN2005):**
```json
{
  "isSuccess": true,
  "code": "LOAN2005",
  "message": "대출 심사 요청에 성공했습니다.",
  "result": {
    "applicationId": 1,
    "productName": "카카오뱅크 개인사업자 신용대출",
    "requestedAmount": 100000000,
    "applied_at": "2024-05-12T14:35:00",
    "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST",
    "notification_enabled": true
  }
}
```
