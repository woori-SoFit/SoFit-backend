# Requirements Document

## Introduction

은행원(BANK_ADMIN)이 대출 신청 상세보기 화면의 My Biz Data 탭에서 신청자의 사업 데이터(연 소득, 보유 대출 건수, 월 매출액, 현금 흐름, 업종 순위 등)를 조회할 수 있는 API를 제공한다. 이 API는 `GET /api/admin/loan-applications/{applicationId}/mybizdata` 엔드포인트로 제공되며, 은행원이 대출 심사 시 신청자의 사업 현황을 파악하는 데 사용된다.

## Glossary

- **MyBiz_Data_Detail_API**: 대출 신청 상세보기 My Biz Data 탭 조회 API. applicationId를 Path Variable로 받아 해당 신청자의 My Biz Data를 반환하는 GET 엔드포인트
- **LoanApplication**: 대출 신청 엔티티. loan_application 테이블에 매핑되며 신청자(user_id), 상품, 신청 금액, 상태(status) 등을 포함
- **MyBizData**: 소상공인 사업자 데이터 엔티티. my_biz_data 테이블에 매핑되며 연 소득, 월 매출액, 현금 흐름, 업종 순위, 세금/보험 상태 등을 포함
- **User**: 사용자 엔티티. users 테이블에 매핑되며 대출 신청자를 식별하는 데 사용
- **ApplicationStatus**: 대출 신청 상태 ENUM. EXECUTED는 대출 실행 완료 상태를 의미
- **VatFilingStatus**: 부가세 신고 상태 ENUM. FILED(신고 완료), PENDING(신고 예정), OVERDUE(기한 초과)
- **InsurancePaymentStatus**: 4대보험 납부 상태 ENUM. PAID(납부 완료), PENDING(납부 예정), OVERDUE(기한 초과)
- **ApiResponse**: 공통 응답 포맷. isSuccess, code, message, result 필드로 구성

## Requirements

### Requirement 1: My Biz Data 탭 조회 API 엔드포인트

**User Story:** 은행원으로서, 대출 신청 건의 My Biz Data 탭을 조회하여, 신청자의 사업 현황 데이터를 확인하고 싶다.

#### Acceptance Criteria

1. WHEN applicationId를 Path Variable로 포함한 GET /api/admin/loan-applications/{applicationId}/mybizdata 요청이 수신되면, THE MyBiz_Data_Detail_API SHALL LoanApplication에서 user_id를 조회하고, 해당 user_id로 MyBizData와 보유 대출 건수를 조회하여 result 필드에 응답 데이터를 포함하는 응답을 반환한다
2. THE MyBiz_Data_Detail_API SHALL 응답을 ApiResponse 공통 포맷(isSuccess, code, message, result)으로 래핑하여 반환한다
3. WHEN 조회가 성공하면, THE MyBiz_Data_Detail_API SHALL HTTP 200 상태 코드와 함께 isSuccess: true, code: "COMMON2000", message: "성공입니다."를 반환한다

### Requirement 2: 응답 데이터 구성

**User Story:** 은행원으로서, 신청자의 소득, 매출, 현금 흐름, 세금 상태 등 핵심 사업 지표를 한 번에 확인하여, 대출 심사 판단 근거로 활용하고 싶다.

#### Acceptance Criteria

1. THE MyBiz_Data_Detail_API SHALL result 필드에 annualIncome(연 소득, Long 타입, 원 단위), existingLoanCount(보유 대출 건수, Integer 타입, 0 이상), monthlyRevenue(월 매출액, Long 타입, 원 단위), monthlyRevenueGrowthRate(전월 대비 증감률, BigDecimal 타입, % 단위), cashFlow(현금 흐름, Long 타입, 원 단위), accountBalance(계좌 잔액, Long 타입, 원 단위), businessAgeMonths(업력, Integer 타입, 개월 단위, 0 이상), vatFilingStatus(부가세 신고 상태, String 타입), taxOverdue(세금 체납 여부, Boolean 타입), insurancePaymentStatus(4대보험 납부 상태, String 타입), industrySalesRank(업종 내 매출 순위, BigDecimal 타입, 상위 %, 0.00~100.00), industryProfitRank(업종 내 수익성 순위, BigDecimal 타입, 상위 %, 0.00~100.00) 12개 필드를 포함한다
2. THE MyBiz_Data_Detail_API SHALL vatFilingStatus 필드를 VatFilingStatus ENUM의 name() 문자열 값(FILED, PENDING, OVERDUE 중 하나)으로 반환한다
3. THE MyBiz_Data_Detail_API SHALL insurancePaymentStatus 필드를 InsurancePaymentStatus ENUM의 name() 문자열 값(PAID, PENDING, OVERDUE 중 하나)으로 반환한다
4. THE MyBiz_Data_Detail_API SHALL monthlyRevenueGrowthRate, industrySalesRank, industryProfitRank 필드를 MyBizData 엔티티의 BigDecimal 원본 값을 변환 없이 그대로 반환한다
5. IF MyBizData 엔티티의 응답 대상 필드 값이 null이면, THEN THE MyBiz_Data_Detail_API SHALL 해당 필드를 JSON null 값으로 포함하여 반환한다

### Requirement 3: 보유 대출 건수(existingLoanCount) 산출 로직

**User Story:** 은행원으로서, 신청자가 현재 보유 중인 대출 건수를 확인하여, 추가 대출 부담을 평가하고 싶다.

#### Acceptance Criteria

1. WHEN MyBiz_Data_Detail_API가 호출되면, THE MyBiz_Data_Detail_API SHALL applicationId로 LoanApplication의 user_id를 조회한 후, 해당 user_id의 LoanApplication 중 status가 EXECUTED인 건의 개수를 existingLoanCount(0 이상의 Integer)로 산출한다
2. THE MyBiz_Data_Detail_API SHALL existingLoanCount 산출 시 현재 조회 대상인 applicationId 건도 EXECUTED 상태이면 카운트에 포함한다
3. IF 해당 user_id의 EXECUTED 상태 LoanApplication이 0건인 경우, THEN THE MyBiz_Data_Detail_API SHALL existingLoanCount를 0으로 반환한다

### Requirement 4: MyBizData 조회 로직

**User Story:** 은행원으로서, 신청자의 최신 사업 데이터를 기반으로 심사하여, 정확한 판단을 내리고 싶다.

#### Acceptance Criteria

1. WHEN MyBiz_Data_Detail_API가 호출되면, THE MyBiz_Data_Detail_API SHALL applicationId로 LoanApplication의 user_id를 조회한 후, 해당 user_id로 MyBizData 엔티티의 최신 데이터(reference_month 기준 내림차순 첫 번째, 동점 시 biz_data_id 내림차순)를 조회하여 응답 필드를 구성한다
2. THE MyBiz_Data_Detail_API SHALL annualIncome, monthlyRevenue, monthlyRevenueGrowthRate, cashFlow, accountBalance, businessAgeMonths, vatFilingStatus, taxOverdue, insurancePaymentStatus, industrySalesRank, industryProfitRank 11개 필드를 MyBizData 엔티티에서 직접 매핑한다

### Requirement 5: 존재하지 않는 리소스 에러 처리

**User Story:** 은행원으로서, 존재하지 않는 applicationId 또는 데이터 미존재 시 명확한 에러 메시지를 받아, 상황을 인지하고 싶다.

#### Acceptance Criteria

1. WHEN 존재하지 않는 applicationId로 요청이 수신되면, THE MyBiz_Data_Detail_API SHALL HTTP 404 상태 코드와 함께 isSuccess: false, code: "COMMON4004", message: "요청한 리소스를 찾을 수 없습니다."를 포함하는 공통 에러 응답을 반환한다
2. IF applicationId에 해당하는 LoanApplication의 user_id로 MyBizData 레코드가 하나도 존재하지 않으면, THEN THE MyBiz_Data_Detail_API SHALL HTTP 404 상태 코드와 함께 isSuccess: false, code: "COMMON4004", message: "요청한 리소스를 찾을 수 없습니다."를 포함하는 공통 에러 응답을 반환한다
3. IF applicationId가 유효하지 않은 형식(숫자가 아닌 값, 음수, 또는 Long 타입 범위를 초과하는 값)이면, THEN THE MyBiz_Data_Detail_API SHALL HTTP 400 상태 코드와 함께 isSuccess: false, code: "COMMON4000", message: "잘못된 요청입니다."를 포함하는 공통 에러 응답을 반환한다
4. THE MyBiz_Data_Detail_API SHALL 에러 처리 우선순위를 인증/권한 검증(401/403) → applicationId 형식 검증(400) → 리소스 존재 여부 검증(404) 순서로 적용한다

### Requirement 6: 인증 및 권한 검증

**User Story:** 은행원으로서, 인증된 관리자만 My Biz Data를 조회할 수 있어, 고객의 사업 데이터가 보호되길 원한다.

#### Acceptance Criteria

1. IF 세션 쿠키가 없거나 세션이 만료 또는 무효한 요청이 수신되면, THEN THE MyBiz_Data_Detail_API SHALL HTTP 401 상태 코드와 함께 GeneralErrorCode.UNAUTHORIZED(code: "COMMON4001", message: "인증이 필요합니다.")를 ApiResponse 공통 포맷(isSuccess: false, code, message, result: null)으로 반환한다
2. IF 인증되었으나 BANK_ADMIN 권한이 없는 사용자가 요청하면, THEN THE MyBiz_Data_Detail_API SHALL HTTP 403 상태 코드와 함께 GeneralErrorCode.FORBIDDEN(code: "COMMON4003", message: "권한이 없습니다.")를 ApiResponse 공통 포맷(isSuccess: false, code, message, result: null)으로 반환한다
3. THE MyBiz_Data_Detail_API SHALL 인증 검증(401)을 권한 검증(403)보다 선행하여 수행하고, 두 검증을 모두 비즈니스 로직 수행 전에 완료하여, 인증/권한 실패 시 My Biz Data에 접근하지 않는다
