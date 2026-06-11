# MyBiz Data 상세 조회 API 수정

## 개요
- **API**: `GET /api/admin/loan-applications/{id}/mybiz-data`
- **목적**: 은행원 대출 심사 화면에서 신청자의 My Biz Data 상세 정보 제공
- **브랜치**: `feat/SOFIT-XXX-mybiz-data-detail-revamp`
- **커밋**: `[SOFIT-XXX] Feat: MyBiz Data 상세 조회 API 응답 확장`

---

## Phase 1: DB 스키마 변경 (MyBizData 엔티티 컬럼 추가)

### 작업 내용
- `my_biz_data` 테이블에 4개 컬럼 추가
- MyBizData 엔티티에 필드 추가
- 더미 데이터 SQL 업데이트

### 추가 컬럼

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `existing_loan_count` | `INTEGER` | 보유 대출 건수 (타행 포함) |
| `annual_repayment` | `BIGINT` | 연간 원리금 상환액 |
| `monthly_repayment` | `BIGINT` | 월 상환액 |
| `total_loan_balance` | `BIGINT` | 총 대출 잔액 |

### 수정 파일
- [ ] `sofit-common/src/main/java/com/sofit/common/entity/mybiz/MyBizData.java`
- [ ] `sofit-common/src/main/resources/sql/data_dummy.sql`

---

## Phase 2: Response DTO 재설계

### 작업 내용
- 기존 `MyBizDataDetailResponse` (단일 record) → 중첩 구조로 변경
- 추이 데이터 (revenueTrend, profitTrend, industryAvgRevenueTrend) 포함
- industryComparison 중첩 객체 포함

### 최종 응답 구조

```json
{
  "existingLoanCount": 1,
  "annualIncome": 130000000,
  "annualRepayment": 30400000,
  "monthlyRepayment": 2530000,
  "totalLoanBalance": 15000000,
  "businessAgeMonths": 18,
  "vatFilingStatus": "FILED",
  "vatFilingDate": "2026-04-25",
  "taxOverdue": false,
  "insurancePaymentStatus": "PAID",
  "revenueTrend": [
    { "referenceMonth": "2024-12", "monthlyRevenue": 9300000 }
  ],
  "profitTrend": [
    { "referenceMonth": "2024-12", "profit": 2300000 }
  ],
  "industryAvgRevenueTrend": [
    { "referenceMonth": "2024-12", "monthlyRevenue": 7700000 }
  ],
  "industryComparison": {
    "myRevenue": 11500000,
    "industryAvgRevenue": 9200000,
    "districtAvgRevenue": 9800000,
    "myProfitRate": 27.0,
    "industryAvgProfitRate": 19.8,
    "districtAvgProfitRate": 21.3,
    "industrySalesRank": 8.2,
    "industryProfitRank": 12.5,
    "industrySatisfactionRank": 15.3,
    "districtSalesRank": 6.8,
    "districtProfitRank": 10.2,
    "districtSatisfactionRank": 11.7
  }
}
```

### DB → 응답 필드 매핑

| 응답 필드 | DB 컬럼 (MyBizData) |
|---|---|
| `existingLoanCount` | `existing_loan_count` (신규) |
| `annualIncome` | `annual_income` |
| `annualRepayment` | `annual_repayment` (신규) |
| `monthlyRepayment` | `monthly_repayment` (신규) |
| `totalLoanBalance` | `total_loan_balance` (신규) |
| `businessAgeMonths` | `business_age_months` |
| `vatFilingStatus` | `vat_filing_status` |
| `vatFilingDate` | `vat_filing_date` |
| `taxOverdue` | `tax_overdue` |
| `insurancePaymentStatus` | `insurance_payment_status` |
| `revenueTrend[].monthlyRevenue` | `monthly_revenue` (6개월 조회) |
| `profitTrend[].profit` | `estimated_profit` (6개월 조회) |
| `industryAvgRevenueTrend[].monthlyRevenue` | `industry_avg_revenue` (6개월 조회) |
| `industryComparison.myRevenue` | `monthly_revenue` (최신 월) |
| `industryComparison.industryAvgRevenue` | `industry_avg_revenue` |
| `industryComparison.districtAvgRevenue` | `district_avg_revenue` |
| `industryComparison.myProfitRate` | `monthly_profit_rate` |
| `industryComparison.industryAvgProfitRate` | `industry_avg_profit_rate` |
| `industryComparison.districtAvgProfitRate` | `district_avg_profit_rate` |
| `industryComparison.industrySalesRank` | `industry_sales_rank` |
| `industryComparison.industryProfitRank` | `industry_profit_rank` |
| `industryComparison.industrySatisfactionRank` | `industry_satisfaction_rank` |
| `industryComparison.districtSalesRank` | `district_sales_rank` |
| `industryComparison.districtProfitRank` | `district_profit_rank` |
| `industryComparison.districtSatisfactionRank` | `district_satisfaction_rank` |

### 수정 파일
- [ ] `sofit-admin/src/main/java/com/sofit/admin/domain/loan/dto/response/MyBizDataDetailResponse.java`

---

## Phase 3: Converter 수정

### 작업 내용
- `MyBizDataDetailConverter` 수정: 6개월 추이 데이터 + industryComparison 매핑 추가
- 기존 `existingLoanCount` 파라미터 제거 (DB 컬럼으로 이동)

### 수정 파일
- [ ] `sofit-admin/src/main/java/com/sofit/admin/domain/loan/converter/MyBizDataDetailConverter.java`

---

## Phase 4: Service 수정

### 작업 내용
- `MyBizDataDetailServiceImpl` 수정
  - 6개월 추이 데이터 조회 로직 추가 (기준 MyBizData의 businessNumber로 이전 5개월 + 기준월 조회)
  - 기존 `countByUser_UserIdAndStatus(EXECUTED)` 로직 제거 (DB 컬럼으로 대체)

### 수정 파일
- [ ] `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/MyBizDataDetailServiceImpl.java`
- [ ] `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/MyBizDataDetailService.java` (인터페이스 변경 없으면 skip)

---

## Phase 5: 기존 테스트 수정

### 작업 내용
- Converter 테스트 수정
- Service 테스트 수정
- Controller 테스트 수정

### 수정 파일
- [ ] `sofit-admin/src/test/java/com/sofit/admin/domain/loan/converter/MyBizDataDetailConverterTest.java`
- [ ] `sofit-admin/src/test/java/com/sofit/admin/domain/loan/service/MyBizDataDetailServiceImplTest.java`
- [ ] `sofit-admin/src/test/java/com/sofit/admin/domain/loan/controller/LoanDashboardControllerTest.java`

---

## 프론트 처리 (BE 응답에서 제외)

| 필드 | 처리 방식 |
|---|---|
| `dsrRate` | 프론트 계산: `annualRepayment / annualIncome * 100` |
| `dsrLimit` | 프론트 상수 (40) |
