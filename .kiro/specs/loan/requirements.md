# Requirements

## Overview
대출 서비스 API. 대출 상품 조회, 신청 가능 여부 확인, 대출 심사 요청 기능을 제공한다.

---

### REQ-1: 대출 상품 목록 조회
**User Story:** 사용자로서, 이용 가능한 대출 상품 목록을 조회하고 싶다.

#### Acceptance Criteria
- [ ] `GET /api/loan-products` 엔드포인트가 존재한다
- [ ] 인증 없이 접근 가능하다 (공개 API)
- [ ] 응답 코드는 `LOAN2001`이며 상품 목록 배열을 반환한다
- [ ] 각 상품은 `productId`, `productName`, `title`을 포함한다

---

### REQ-2: 대출 상품 상세 조회
**User Story:** 사용자로서, 특정 대출 상품의 상세 정보를 조회하고 싶다.

#### Acceptance Criteria
- [ ] `GET /api/loan-products/{productId}` 엔드포인트가 존재한다
- [ ] path variable로 `productId`를 받는다
- [ ] 응답 코드는 `LOAN2002`이며 단일 상품 상세를 반환한다
- [ ] 응답에 `productId`, `productName`, `title`, `subtitle`, `maxLimit`, `maxTerm`, `industryType`, `interest_rate(minRate, maxRate)`를 포함한다
- [ ] 존재하지 않는 productId는 404를 반환한다

---

### REQ-3: 신청 가능 여부 확인
**User Story:** 사용자로서, 특정 대출 상품에 대한 나의 신청 가능 여부를 확인하고 싶다.

#### Acceptance Criteria
- [ ] `POST /api/loan-products/{productId}/check` 엔드포인트가 존재한다
- [ ] `Content-Type: application/json` 헤더가 필요하다
- [ ] Request Body에 아래 4개 ENUM 필드를 받는다:
  - `user_input_annual_income`: `AMT_0_30M` / `AMT_30_50M` / `AMT_50_100M` / `AMT_100M_OVER`
  - `user_input_credit_score`: `CS_0_850` / `CS_850_OVER` / `CS_UNKNOWN`
  - `user_input_income_type`: `01`(급여) / `02`(사업) / `03`(기타)
  - `user_input_existing_loan_amt`: `LOAN_100M_OVER` / `LOAN_0_100M` / `LOAN_NONE`
- [ ] 신청 가능 시 응답 코드 `LOAN2003`, `eligible: true`, `application_id` 반환
- [ ] 신청 불가 시 응답 코드 `LOAN2004`, `eligible: false`, `application_id` 반환
- [ ] 두 경우 모두 HTTP status는 200

---

### REQ-4: 대출 심사 요청
**User Story:** 사용자로서, 대출 상품에 대한 심사를 요청하고 싶다.

#### Acceptance Criteria
- [ ] `POST /api/loan-applications/{applicationId}/submit` 엔드포인트가 존재한다
- [ ] `Content-Type: application/json` 헤더가 필요하다
- [ ] Request Body에 아래 필드를 받는다:
  - `requestedAmount`: 신청 금액 (숫자)
  - `requestedTerm`: 신청 기간 (숫자, 개월)
  - `repaymentMethod`: `EQUAL_PRINCIPAL_INTEREST` 등 상환 방식 ENUM
  - `purpose`: `WORKING_CAPITAL` 등 대출 목적 ENUM
- [ ] 응답 코드는 `LOAN2005`이며 심사 요청 결과를 반환한다
- [ ] 응답에 `applicationId`, `productName`, `requestedAmount`, `applied_at`, `repaymentMethod`, `notification_enabled`를 포함한다
- [ ] 존재하지 않는 applicationId는 404를 반환한다
