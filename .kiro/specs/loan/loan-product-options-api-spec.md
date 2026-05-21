# 대출 상품 옵션 조회 API 명세서

**API 명**: Loan Product Options API  


---

## 목차

1. [배경 및 설계 원칙](#1-배경-및-설계-원칙)
2. [DB 설계](#2-db-설계)
3. [API 상세 - 단건 조회](#3-api-상세--단건-조회)
4. [에러 응답](#4-에러-응답)
5. [코드 정의](#5-코드-정의)
6. [유의사항](#6-유의사항)

---

## 1. 배경 및 설계 원칙

대출 신청 화면에서 상품별 **자금용도 / 상환방식 / 최대 대출기간 / 대출 한도**를 서버가 내려주고, 프론트엔드는 이를 기반으로 선택 UI를 동적으로 구성한다.

**프론트 하드코딩 분기 처리를 금지하는 이유**

- 상품 조건이 변경될 때마다 프론트 배포가 필요해져 운영 리스크 증가
- 서버 응답만 수정하면 프론트 재배포 없이 즉시 반영 가능

**처리 흐름**

```
서버  →  상품별 가능 용도 / 상환방식 / 최대기간 / 대출 한도 응답
프론트 →  내려받은 옵션으로 선택 UI 구성 + 1차 validation
서버  →  최종 신청 시 서버에서 재검증
```

---

## 2. DB 설계

### 테이블 관계

기존 `loan_products` 테이블은 상품 기본 정보만 보유한다.  
자금용도 × 상환방식 × 최대기간 **조합 데이터는 별도 테이블** `loan_product_options`에 관리한다.  
`minLimit` / `maxLimit`은 `loan_products` 테이블의 `min_limit` / `max_limit` 컬럼을 그대로 응답에 포함한다.

```
loan_products (1) ──────< loan_product_options (N)
  product_id   (PK)          option_id        (PK)
  product_name               product_id       (FK)
  min_rate                   purpose
  max_rate                   repayment_method ← RepaymentMethod (단일 Enum)
  min_limit   ← 응답에 포함  max_term_months
  max_limit   ← 응답에 포함
  max_term  ← 상품 전체 절대 최대값 (참고용)
  ...
```

> `loan_products.max_term` : 상품 전체의 절대 최대 기간 (참고용)  
> `loan_product_options.max_term_months` : purpose + repayment_method **조합별** 적용 제한값

### loan_product_options 테이블 DDL

```sql
CREATE TABLE loan_product_options (
    option_id        BIGINT       NOT NULL AUTO_INCREMENT,
    product_id       BIGINT       NOT NULL,
    purpose          ENUM('WORKING_CAPITAL', 'FACILITY_CAPITAL')              NOT NULL,
    repayment_method ENUM('BULLET', 'EQUAL_PRINCIPAL', 'EQUAL_PAYMENT') NOT NULL,
    max_term_months  INT          NOT NULL,
    PRIMARY KEY (option_id),
    FOREIGN KEY (product_id) REFERENCES loan_products(product_id)
);
```

### 초기 데이터 INSERT

```sql
-- 우리 Oh!(5) 클릭 대출 (product_id = 2)
INSERT INTO loan_product_options (product_id, purpose, repayment_method, max_term_months) VALUES
(2, 'WORKING_CAPITAL', 'BULLET',           12),  -- 운전자금 / 만기일시상환 / 1년
(2, 'WORKING_CAPITAL', 'EQUAL_PRINCIPAL',  60),  -- 운전자금 / 원금균등상환 / 5년
(2, 'WORKING_CAPITAL', 'EQUAL_PAYMENT',    60);  -- 운전자금 / 원리금균등상환 / 5년

-- 우리 사장님 대출 (product_id = 1)
INSERT INTO loan_product_options (product_id, purpose, repayment_method, max_term_months) VALUES
(1, 'WORKING_CAPITAL', 'BULLET',           12),  -- 운전자금 / 만기일시상환 / 1년
(1, 'WORKING_CAPITAL', 'EQUAL_PRINCIPAL',  60),  -- 운전자금 / 원금균등상환 / 5년
(1, 'WORKING_CAPITAL', 'EQUAL_PAYMENT',    60),  -- 운전자금 / 원리금균등상환 / 5년
(1, 'FACILITY_CAPITAL',        'BULLET',           36),  -- 시설자금 / 만기일시상환 / 3년
(1, 'FACILITY_CAPITAL',        'EQUAL_PRINCIPAL', 240),  -- 시설자금 / 원금균등상환 / 20년
(1, 'FACILITY_CAPITAL',        'EQUAL_PAYMENT',   240);  -- 시설자금 / 원리금균등상환 / 20년
```

### 조회 쿼리 (단건)

```sql
SELECT
    p.product_id,
    p.product_name,
    p.min_limit,
    p.max_limit,
    o.purpose,
    o.repayment_method,
    o.max_term_months
FROM loan_products p
JOIN loan_product_options o ON p.product_id = o.product_id
WHERE p.product_id = :productId;
```

---

## 3. API 상세 - 단건 조회

### Request

| 항목 | 내용 |
|------|------|
| Method | `GET` |
| URL | `/api/v1/loan-products/{productId}/options` |
| 인증 | 세션 (Session Cookie) |

#### Path Parameters

| 파라미터 | 타입 | 필수 | 설명 |
|----------|------|:----:|------|
| `productId` | integer | Y | 대출 상품 ID |

#### Request Example

```http
GET /api/v1/loan-products/1/options
Cookie: JSESSIONID={session_id}
```

---

### Response

#### 성공 (200 OK) — 우리 사장님 대출

```json
{
  "productId": 1,
  "productName": "우리 사장님 대출",
  "minLimit": 5000000,
  "maxLimit": 100000000,
  "loanOptions": [
    { "purpose": "WORKING_CAPITAL", "repaymentMethod": "BULLET",          "maxTermMonths": 12  },
    { "purpose": "WORKING_CAPITAL", "repaymentMethod": "EQUAL_PRINCIPAL", "maxTermMonths": 60  },
    { "purpose": "WORKING_CAPITAL", "repaymentMethod": "EQUAL_PAYMENT",   "maxTermMonths": 60  },
    { "purpose": "FACILITY_CAPITAL",        "repaymentMethod": "BULLET",          "maxTermMonths": 36  },
    { "purpose": "FACILITY_CAPITAL",        "repaymentMethod": "EQUAL_PRINCIPAL", "maxTermMonths": 240 },
    { "purpose": "FACILITY_CAPITAL",        "repaymentMethod": "EQUAL_PAYMENT",   "maxTermMonths": 240 }
  ]
}
```

#### 성공 (200 OK) — 우리 Oh!(5) 클릭 대출

```json
{
  "productId": 2,
  "productName": "우리 Oh!(5) 클릭 대출",
  "minLimit": 1000000,
  "maxLimit": 30000000,
  "loanOptions": [
    { "purpose": "WORKING_CAPITAL", "repaymentMethod": "BULLET",          "maxTermMonths": 12 },
    { "purpose": "WORKING_CAPITAL", "repaymentMethod": "EQUAL_PRINCIPAL", "maxTermMonths": 60 },
    { "purpose": "WORKING_CAPITAL", "repaymentMethod": "EQUAL_PAYMENT",   "maxTermMonths": 60 }
  ]
}
```

#### Response Fields

| 필드 | 타입 | 설명 |
|------|------|------|
| `productId` | integer | 상품 ID |
| `productName` | string | 상품명 |
| `minLimit` | integer | 최소 대출 가능 금액 (원) |
| `maxLimit` | integer | 최대 대출 한도 (원) |
| `loanOptions` | array | 허용된 옵션 조합 목록 |
| `loanOptions[].purpose` | string (enum) | 자금용도 → [코드 정의 참고](#5-코드-정의) |
| `loanOptions[].repaymentMethod` | string (enum) | 상환방식 → [코드 정의 참고](#5-코드-정의) |
| `loanOptions[].maxTermMonths` | integer | 해당 조합의 최대 대출기간 (단위: 개월) |

---

## 4. 에러 응답

모든 에러는 아래 공통 포맷을 따른다.

```json
{
  "isSuccess": false,
  "code": "PRODUCT_NOT_FOUND",
  "message": "존재하지 않는 상품입니다."
}
```

| HTTP Status | code | 설명 |
|:-----------:|------|------|
| `400` | `INVALID_PRODUCT_ID` | productId 형식 오류 |
| `401` | `UNAUTHORIZED` | 인증 토큰 없음 또는 만료 |
| `404` | `PRODUCT_NOT_FOUND` | 해당 productId의 상품 없음 |
| `500` | `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

---

## 5. 코드 정의

### purpose (자금용도)

| 코드 | 설명 |
|------|------|
| `WORKING_CAPITAL` | 운전자금 |
| `FACILITY_CAPITAL` | 시설자금 |

---

### repaymentMethod (상환방식)

옵션 조회 응답과 신청 제출 요청 모두 **동일한 단일 Enum**을 사용한다.  
`INSTALLMENT`(분할상환 그룹) 같은 중간 코드 없이, 프론트는 받은 값 그대로 버튼을 렌더링한다.

```kotlin
enum class RepaymentMethod {
    BULLET,           // 만기일시상환
    EQUAL_PRINCIPAL,  // 원금균등상환
    EQUAL_PAYMENT     // 원리금균등상환
}
```

| 코드 | 설명 | 분류 |
|------|------|------|
| `BULLET` | 만기일시상환 | 일시상환 |
| `EQUAL_PRINCIPAL` | 원금균등상환 | 분할상환 |
| `EQUAL_PAYMENT` | 원리금균등상환 | 분할상환 |

---

## 6. 유의사항

- 서버는 대출 신청 최종 제출 시 `loanOptions` 조건을 **서버 측에서 재검증**한다. 프론트 1차 validation만으로 신뢰하지 않는다.
- `loanOptions`는 허용된 조합의 배열이므로, 같은 `purpose`에 `repaymentMethod`가 복수일 수 있다.
- `maxTermMonths`는 `purpose + repaymentMethod` 조합 단위로 독립 적용된다.
- `loan_products.max_term`(상품 테이블)은 상품 전체의 절대 최대값으로, `loan_product_options.max_term_months`와 구분해서 관리한다.
