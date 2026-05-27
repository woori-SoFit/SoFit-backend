# Design

## API Overview

| # | 기능 | Method | Path | 코드 |
|---|---|---|---|---|
| 1 | 대출 상품 목록 조회 | GET | /api/loan-products | LOAN2001 |
| 2 | 대출 상품 상세 조회 | GET | /api/loan-products/{productId} | LOAN2002 |
| 3 | 대출 신청 생성 (1차 필터링 통과 후) | POST | /api/loan-products/{productId}/applications | LOAN2003 |
| 4 | DRAFT 존재 여부 확인 | GET | /api/loan-applications/draft?productId={productId} | LOAN2004 |
| 5 | 이어가기 데이터 조회 | GET | /api/loan-applications/{applicationId}/resume | LOAN2011 |
| 6 | 최종 제출 (심사 요청) | POST | /api/loan-applications/{applicationId}/submit | LOAN2005 |

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
      { "productId": 2, "productName": "우리카드 가맹점 우대 대출", "title": "매출대금 입금 중인 개인사업자 전용 대출" }
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

### 3. 대출 신청 생성 (1차 필터링 통과 후)
**POST** `/api/loan-products/{productId}/applications`

> 1차 필터링은 프론트엔드에서 처리. 백엔드는 통과한 사용자의 요청값을 그대로 DRAFT 상태로 저장.

**Request Body:**
```json
{
  "annualIncome": "AMT_0_30M",
  "creditScore": "CS_850_OVER",
  "incomeType": "02",
  "existingLoanAmt": "LOAN_0_100M"
}
```

**처리 로직:**
- 세션에서 userId 추출
- 상품 존재 + ACTIVE 확인
- 동일 상품 중복 신청 체크 (CANCELLED 제외)
- DRAFT 상태로 loan_application 레코드 생성

**Response (200, code: LOAN2003):**
```json
{
  "isSuccess": true,
  "code": "LOAN2003",
  "message": "대출 신청이 생성되었습니다.",
  "result": {
    "applicationId": 10293
  }
}
```

**에러 응답:**
- 404: 존재하지 않는 상품
- 400: 비활성 상품
- 409: 동일 상품 중복 신청

---

### 4. DRAFT 존재 여부 확인
**GET** `/api/loan-applications/draft?productId={productId}`

> 특정 상품에 대해 현재 사용자의 DRAFT 상태 신청이 있는지 확인 (이어가기 모달 표시용)

**Response (200, code: LOAN2004):**
```json
{
  "isSuccess": true,
  "code": "LOAN2004",
  "message": "DRAFT 조회에 성공했습니다.",
  "result": {
    "hasDraft": true,
    "applicationId": 10293,
    "lastCompletedStep": "CONSENT_DONE",
    "resumeStep": "AUTH"
  }
}
```

---

### 5. 이어가기 데이터 조회
**GET** `/api/loan-applications/{applicationId}/resume`

> DRAFT 상태인 신청의 저장된 데이터를 반환하여 프론트에서 화면 복원에 사용

**Response (200, code: LOAN2011):**
```json
{
  "isSuccess": true,
  "code": "LOAN2011",
  "message": "이어가기 데이터 조회에 성공했습니다.",
  "result": {
    "applicationId": 10293,
    "resumeStep": "AUTH",
    "savedData": {
      "annualIncome": "AMT_0_30M",
      "creditScore": "CS_850_OVER",
      "incomeType": "02",
      "existingLoanAmt": "LOAN_0_100M",
      "consentsAgreed": true
    }
  }
}
```

---

### 6. 최종 제출 (심사 요청)
**POST** `/api/loan-applications/{applicationId}/submit`

> DRAFT → SUBMITTED 상태 변경. applied_at 기록.

**Request Body:**
```json
{
  "purpose": "WORKING_CAPITAL",
  "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST",
  "requestedTerm": 60,
  "requestedAmount": 100000000
}
```

**처리 로직:**
- 세션에서 userId 추출
- applicationId + userId로 본인 소유 확인
- status가 DRAFT인지 확인
- application.submit() 호출 (status → SUBMITTED, appliedAt 기록)

**Response (200, code: LOAN2005):**
```json
{
  "isSuccess": true,
  "code": "LOAN2005",
  "message": "대출 심사 요청에 성공했습니다.",
  "result": {
    "applicationId": 1,
    "productName": "우리 Oh!(5)클릭 대출",
    "requestedAmount": 100000000,
    "appliedAt": "2024-05-12T14:35:00",
    "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST"
  }
}
```

**에러 응답:**
- 404: 존재하지 않는 신청
- 400: DRAFT 상태가 아닌 신청

---

## 대출 신청 플로우 단계별 API (다른 팀원 개발 완료)

> 아래 API는 대출 신청 플로우의 중간 단계로, 각 단계 완료 시 `lastCompletedStep`을 갱신한다.

```
POST /api/loan-applications/{applicationId}/consents      // 약관 동의
POST /api/loan-applications/{applicationId}/auth          // 본인인증 (금융인증서)
GET  /api/loan-applications/{applicationId}/biz-info      // 사업자 정보 확인
POST /api/loan-applications/{applicationId}/collect-data  // 마이데이터 수집
```

---

## lastCompletedStep 흐름

```
null → CONSENT → AUTH → BIZ_INFO → COLLECT_DATA → MYBIZ → LOAN_CONDITION → SUBMIT
```

| lastCompletedStep | 다음 단계 (resumeStep) |
|---|---|
| null (DRAFT 생성 직후) | CONSENT |
| CONSENT_DONE | AUTH |
| AUTH_DONE | BIZ_INFO |
| BIZ_INFO_DONE | COLLECT_DATA |
| DATA_COLLECTED | MYBIZ |
| MYBIZ_CONNECTED | LOAN_CONDITION |
| LOAN_CONDITION_DONE | SUBMIT |
