# Database Schema

## Common 모듈 엔티티
> `loan_product`, `loan_application` 모두 고객/은행원 공통 도메인이므로 `common` 모듈에 위치

---

## 1. 대출 상품 `loan_product`
> 은행원이 등록·수정·비활성화 관리하는 대출 상품 목록.
> **BaseTimeEntity 상속** (created_at, updated_at 포함)

```sql
CREATE TABLE loan_product (
    product_id               BIGINT          AUTO_INCREMENT PRIMARY KEY,
    product_name             VARCHAR(100)    NOT NULL,
    description              TEXT,
    status                   ENUM('ACTIVE', 'INACTIVE') NOT NULL,
    min_rate                 DECIMAL(5,2)    NOT NULL,
    max_rate                 DECIMAL(5,2)    NOT NULL,
    max_limit                BIGINT          NOT NULL,
    min_term                 INT             NOT NULL,
    max_term                 INT             NOT NULL,
    title                    VARCHAR(100),
    subtitle                 VARCHAR(100),
    industry_type            VARCHAR(50),
    min_business_age_months  INT,
    annual_income_limit      DECIMAL(18,0),
    income_type_code_limit   CHAR(2),
    credit_score_limit       SMALLINT,
    existing_loan_amt_limit  DECIMAL(18,0),
    scb_limit                INT,
    created_by               BIGINT          NOT NULL,
    created_at               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by) REFERENCES users(user_id)
);
```

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| product_id | BIGINT | 대출 상품 고유 ID (PK) |
| product_name | VARCHAR(100) | 상품명 |
| description | TEXT | 상품 상세 설명 |
| status | ENUM | `ACTIVE` 판매중 / `INACTIVE` 판매중단 (비활성화 시 신규 신청 차단) |
| min_rate | DECIMAL(5,2) | 최저 금리 (%) |
| max_rate | DECIMAL(5,2) | 최고 금리 (%) |
| max_limit | BIGINT | 최대 대출 한도 (원) |
| min_term | INT | 최소 대출 기간 (개월) |
| max_term | INT | 최대 대출 기간 (개월) |
| title | VARCHAR(100) | 화면 표시용 텍스트 |
| subtitle | VARCHAR(100) | 화면 표시용 텍스트 설명 |
| industry_type | VARCHAR(50) | 대상 업종 (NULL이면 업종 제한 없음) |
| min_business_age_months | INT | 최소 업력 조건 (개월, NULL이면 제한 없음) |
| annual_income_limit | DECIMAL(18,0) | 1차 필터링용 연소득 (최대 1,000조 미만까지 수용) |
| income_type_code_limit | CHAR(2) | 1차 필터링용 소득 종류 코드 (01: 사업, 02: 근로 …) |
| credit_score_limit | SMALLINT | 1차 필터링용 신용점수 |
| existing_loan_amt_limit | DECIMAL(18,0) | 1차 필터링용 기보유 대출액 |
| scb_limit | INT | 심사에 필요한 SCB 점수 제한 |
| created_by | BIGINT | 상품 등록 은행원 ID (FK → users) |
| created_at | DATETIME | 상품 등록 일시 |
| updated_at | DATETIME | 최종 수정 일시 |

---

## 2. 대출 신청 `loan_application`
> 대출 신청부터 실행 완료까지 전체 생명주기를 관리하는 핵심 테이블.

```sql
CREATE TABLE loan_application (
    application_id              BIGINT      AUTO_INCREMENT PRIMARY KEY,
    user_id                     BIGINT      NOT NULL,
    product_id                  BIGINT      NOT NULL,
    biz_data_id                 BIGINT,
    s_evaluation_id             BIGINT,
    user_input_annual_income    ENUM('AMT_0_30M', 'AMT_30_50M', 'AMT_50_100M', 'AMT_100M_OVER'),
    user_input_credit_score     ENUM('CS_0_850', 'CS_850_OVER', 'CS_UNKNOWN'),
    user_input_income_type      ENUM('01', '02', '03'),
    user_input_existing_loan_amt ENUM('LOAN_100M_OVER', 'LOAN_0_100M', 'LOAN_NONE'),
    requested_amount            BIGINT,
    requested_term              INT,
    cb_credit_score             INT,
    purpose                     ENUM('WORKING_CAPITAL', 'FACILITY_CAPITAL'),
    repayment_method            ENUM('EQUAL_PRINCIPAL_INTEREST', 'EQUAL_PRINCIPAL', 'BALLOON'),
    status                      ENUM(
                                    'DRAFT', 'SUBMITTED', 'CB_CHECKING',
                                    'BASIC_REVIEW', 'SCB_CALCULATING', 'FINAL_REVIEW',
                                    'APPROVED', 'REJECTED', 'CONTRACTED', 'EXECUTED', 'CANCELLED'
                                ) NOT NULL DEFAULT 'DRAFT',
    applied_at                  DATETIME,
    updated_at                  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)    REFERENCES users(user_id),
    FOREIGN KEY (product_id) REFERENCES loan_product(product_id)
);
```

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| application_id | BIGINT | 대출 신청 고유 ID (PK) |
| user_id | BIGINT | 신청 고객 ID (FK → users) |
| product_id | BIGINT | 신청 대출 상품 ID (FK → loan_product) |
| biz_data_id | BIGINT | 신청 시 연동된 My Biz Data ID (FK → my_biz_data) |
| s_evaluation_id | BIGINT | 신청 시 산출된 S 등급 ID (FK → s_evaluations) |
| user_input_annual_income | ENUM | 고객 입력 연소득: `AMT_0_30M` / `AMT_30_50M` / `AMT_50_100M` / `AMT_100M_OVER` |
| user_input_credit_score | ENUM | 고객 입력 신용점수: `CS_0_850` / `CS_850_OVER` / `CS_UNKNOWN` |
| user_input_income_type | ENUM | 고객 입력 소득 종류: `01` 급여 / `02` 사업 / `03` 기타 |
| user_input_existing_loan_amt | ENUM | 고객 입력 기보유 대출액: `LOAN_100M_OVER` / `LOAN_0_100M` / `LOAN_NONE` |
| requested_amount | BIGINT | 대출 금액 (원) |
| requested_term | INT | 대출 기간 (개월) |
| cb_credit_score | INT | CB 점수 |
| purpose | ENUM | 자금 용도: `WORKING_CAPITAL` 운전자금 / `FACILITY_CAPITAL` 시설자금 |
| repayment_method | ENUM | 상환방식: `EQUAL_PRINCIPAL_INTEREST` 원리금균등 / `EQUAL_PRINCIPAL` 원금균등 / `BALLOON` 만기상환 |
| status | ENUM | 심사 단계: `DRAFT` → `SUBMITTED` → `CB_CHECKING` → `BASIC_REVIEW` → `SCB_CALCULATING` → `FINAL_REVIEW` → `APPROVED` / `REJECTED` → `CONTRACTED` → `EXECUTED` / `CANCELLED` |
| applied_at | DATETIME | 최종 대출 신청 완료 일시 (created_at과 별개: 1차 필터링 '확인하기' 시점에 레코드 생성, 최종 신청 완료 시 applied_at 기록) |
| updated_at | DATETIME | 최종 상태 변경 일시 |

### status 생명주기
```
DRAFT → SUBMITTED → CB_CHECKING → BASIC_REVIEW → SCB_CALCULATING → FINAL_REVIEW
                                                                         ├── APPROVED → CONTRACTED → EXECUTED
                                                                         └── REJECTED
※ 어느 단계에서든 CANCELLED 가능
```
