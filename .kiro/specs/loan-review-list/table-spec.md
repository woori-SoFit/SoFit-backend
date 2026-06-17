# SOFIT-32 관련 테이블 명세

## 대출 신청 `loan_application`

> 대출 신청부터 실행 완료까지 전체 생명주기를 관리하는 핵심 테이블.

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| application_id | BIGINT | 대출 신청 고유 ID (PK) |
| user_id | BIGINT | 신청 고객 ID (FK → users) |
| product_id | BIGINT | 신청 대출 상품 ID (FK → loan_product) |
| biz_data_id | BIGINT | 신청 시 연동된 My Biz Data ID (FK → my_biz_data) |
| scb_evaluation_id | BIGINT | 신청 시 산출된 SCB 등급 ID (FK → s_evaluations, NULL 허용) |
| user_input_annual_income | ENUM | 고객 입력 연소득: `AMT_0_30M` / `AMT_30_50M` / `AMT_50_100M` / `AMT_100M_OVER` |
| user_input_credit_score | ENUM | 고객 입력 신용점수: `CS_0_850` / `CS_850_OVER` / `CS_UNKNOWN` |
| user_input_income_type | ENUM | 고객 입력 소득 종류: `01` 급여 / `02` 사업 / `03` 기타 |
| user_input_existing_loan_amt | DECIMAL(18,0) | 고객 입력 기보유 대출액 (원) |
| requested_amount | BIGINT | 대출 금액 (원) |
| requested_term | INT | 대출 기간 (개월) |
| cb_credit_score | INT | CB 점수 |
| purpose | ENUM | 자금 용도: `WORKING_CAPITAL` 운전자금 / `FACILITY_CAPITAL` 시설자금 |
| repayment_method | ENUM | 상환방식: `EQUAL_PRINCIPAL_INTEREST` 원리금균등 / `EQUAL_PRINCIPAL` 원금균등 / `BALLOON` 만기상환 |
| status | ENUM | 심사 단계: `DRAFT` / `SUBMITTED` / `CB_CHECKING` / `BASIC_REVIEW` / `SCB_CALCULATING` / `FINAL_REVIEW` / `APPROVED` / `REJECTED` / `CONTRACTED` / `EXECUTED` / `CANCELLED` |
| applied_at | DATETIME | 신청 일시 (최종 대출 신청 완료 후 생성, created_at과 다름) |
| updated_at | DATETIME | 최종 상태 변경 일시 |

---

## 대출 상품 `loan_product`

> 은행원이 등록·수정·비활성화 관리하는 대출 상품 목록. BaseTimeEntity 상속.
> **팀원이 이미 엔티티 생성 완료 — import만 해서 사용할 것.**

| 컬럼명 | 타입 | NULL | 설명 |
|---|---|---|---|
| product_id | BIGINT | NOT NULL | 대출 상품 고유 ID (PK) |
| product_name | VARCHAR(100) | NOT NULL | 상품명 ⭐ 목록/상세 응답에 사용 |
| description | TEXT | NOT NULL | 상품 상세 설명 |
| status | ENUM('ACTIVE','INACTIVE') | NOT NULL | `ACTIVE` 판매중 / `INACTIVE` 판매중단 |
| min_rate | DECIMAL(5,2) | NOT NULL | 최저 금리 (%) |
| max_rate | DECIMAL(5,2) | NOT NULL | 최고 금리 (%) |
| max_limit | BIGINT | NOT NULL | 최대 대출 한도 (원) |
| min_term | INT | NOT NULL | 최소 대출 기간 (개월) |
| max_term | INT | NOT NULL | 최대 대출 기간 (개월) |
| title | VARCHAR(100) | NOT NULL | 화면 표시용 제목 |
| subtitle | VARCHAR(100) | NOT NULL | 화면 표시용 부가설명 |
| industry_type | VARCHAR(50) | NULL | 대상 업종 (NULL이면 업종 제한 없음) |
| min_business_age_months | INT | NULL | 최소 업력 조건 (개월, NULL이면 제한 없음) |
| annual_income_limit | DECIMAL(18,0) | NOT NULL | 1차 필터링용 연소득 |
| credit_score_limit | SMALLINT | NOT NULL | 1차 필터링용 신용점수 |
| income_type_code_limit | CHAR(2) | NOT NULL | 1차 필터링용 소득 종류 코드 |
| existing_loan_amt_limit | DECIMAL(18,0) | NOT NULL | 1차 필터링용 기보유 대출액 |
| scb_limit | INT | NOT NULL | 심사에 필요한 SCB 등급 제한 |
| created_at | DATETIME | NOT NULL | 상품 등록 일시 |
| created_by | BIGINT | NOT NULL | 상품 등록 은행원 ID (FK → users) |
| updated_at | DATETIME | NOT NULL | 최종 수정 일시 |

---

## 테이블 관계

```
loan_application.product_id → loan_product.product_id
```

---

## 심사 중 상태 범위

목록 조회 시 아래 status만 포함한다.

```
SUBMITTED, CB_CHECKING, BASIC_REVIEW, SCB_CALCULATING, FINAL_REVIEW
```

---

## 주의사항

- `applied_at`: 최종 대출 신청 완료 시점. `created_at`(1차 필터링 정보 입력 시점)과 다름
- `loan_application`은 BaseEntity 미상속 — `created_at` 컬럼 없음