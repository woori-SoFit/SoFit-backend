# Requirements Document

## Introduction

로그인한 소상공인 사용자가 자신의 My Biz Data 대시보드를 조회하는 API를 제공한다. `GET /api/my-biz/dashboard` 엔드포인트를 통해 월 매출, 매출 증감률, 현금흐름, 순이익 추정, 업종 비교, 매출 추이, 현금흐름 추이, 네이버 평점, 리뷰 수, 배달 재주문율, 배달 주문 수를 반환한다. 세션 기반 인증을 사용하며, 조회 기준월을 선택적으로 지정할 수 있다.

## Glossary

- **MyBiz_Dashboard_API**: 로그인한 사용자의 My Biz Data 대시보드 정보를 조회하는 REST API 엔드포인트 (`GET /api/my-biz/dashboard`)
- **MyBizData**: 소상공인 사업자 데이터를 저장하는 엔티티. my_biz_data 테이블에 매핑되며 월별 매출, 현금흐름, 업종 순위 등을 포함
- **Reference_Month**: 데이터 조회 기준월. yyyy-MM 형식의 문자열로 요청하며, DB에는 해당 월의 1일(yyyy-MM-01) DATE 타입으로 저장
- **Revenue_Trend**: 조회 기준월 포함 이전 최대 5개월간의 월별 매출 추이 데이터 목록
- **CashFlow_Trend**: 조회 기준월 포함 이전 최대 3개월간의 월별 입금액/출금액 추이 데이터 목록
- **Industry_Compare**: 동일 업종 내 매출, 수익성, 안정성 상위 백분율 비교 데이터
- **MyBizConverter**: MyBizData 엔티티를 Response DTO로 변환하는 역할을 담당하는 클래스
- **MyBizDataRepository**: sofit-common 모듈에 위치하며 MyBizData 엔티티의 데이터 접근을 담당하는 JPA Repository
- **Session**: Redis에 저장되는 사용자 인증 세션. 로그인 시 생성되며 사용자 식별에 사용

## Requirements

### Requirement 1: 대시보드 기본 데이터 조회

**User Story:** 소상공인 사용자로서, 내 사업 현황 대시보드를 조회하고 싶다. 그래서 월 매출, 매출 증감률, 현금흐름, 순이익 추정, 업종 비교, 네이버 평점, 리뷰 수, 배달 재주문율, 배달 주문 수를 한눈에 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 사용자가 `GET /api/my-biz/dashboard`를 요청하면, THE MyBiz_Dashboard_API SHALL HTTP 200 상태 코드와 함께 응답 코드 `COMMON2000`, 메시지 `성공입니다.`를 반환한다.
2. WHEN 인증된 사용자가 `GET /api/my-biz/dashboard`를 요청하면, THE MyBiz_Dashboard_API SHALL `result` 필드에 referenceMonth(String), monthlyRevenue(Long), monthlyRevenueGrowthRate(BigDecimal), cashFlow(Long), estimatedProfit(Long), industryCompare(Object), revenueTrend(List), cashFlowTrend(List), naverRating(BigDecimal), reviewCount(Integer), deliveryReorderRate(BigDecimal), deliveryOrderCount(Integer)를 포함하여 반환한다.
3. WHEN 인증된 사용자가 `GET /api/my-biz/dashboard`를 요청하면, THE MyBiz_Dashboard_API SHALL referenceMonth를 `yyyy-MM` 형식의 문자열로 반환한다.
4. WHEN 인증된 사용자가 `GET /api/my-biz/dashboard`를 요청하면, THE MyBiz_Dashboard_API SHALL industryCompare 필드에 industrySalesRank(BigDecimal, 동일 업종 내 매출 상위 백분율), industryProfitRank(BigDecimal, 동일 업종 내 수익성 상위 백분율), industryStabilityRank(BigDecimal, 동일 업종 내 안정성 상위 백분율)를 포함하여 반환한다.
5. WHEN 인증된 사용자가 `GET /api/my-biz/dashboard`를 요청하면, THE MyBiz_Dashboard_API SHALL monthlyRevenueGrowthRate를 전월 대비 매출 증감률(백분율, 소수점 이하 2자리)로 반환하고, naverRating을 0.0~5.0 범위의 소수점 이하 1자리 값으로 반환하고, deliveryReorderRate를 0.0~100.0 범위의 백분율(소수점 이하 2자리)로 반환한다.

### Requirement 2: 기준월 파라미터 처리

**User Story:** 소상공인 사용자로서, 특정 월의 대시보드 데이터를 조회하고 싶다. 그래서 과거 월별 사업 현황을 비교 분석할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 사용자가 month 파라미터 없이 `GET /api/my-biz/dashboard`를 요청하면, THE MyBiz_Dashboard_API SHALL 해당 사용자의 my_biz_data 테이블에서 reference_month가 가장 큰(MAX) 데이터를 기준으로 대시보드를 반환한다.
2. WHEN 인증된 사용자가 month 파라미터(yyyy-MM 형식)와 함께 `GET /api/my-biz/dashboard?month=2024-05`를 요청하면, THE MyBiz_Dashboard_API SHALL 파라미터 값을 해당 월 1일(yyyy-MM-01) DATE로 변환하여 일치하는 reference_month의 데이터를 조회하고 대시보드를 반환한다.
3. IF month 파라미터가 yyyy-MM 정규표현식(`^\d{4}-(0[1-9]|1[0-2])$`)에 매칭되지 않는 값으로 요청되면, THEN THE MyBiz_Dashboard_API SHALL HTTP 400 상태 코드와 함께 `COMMON4000` 에러 코드를 반환한다.
4. IF month 파라미터가 빈 문자열로 요청되면, THEN THE MyBiz_Dashboard_API SHALL month 파라미터가 없는 경우와 동일하게 가장 최신 reference_month 데이터를 기준으로 대시보드를 반환한다.

### Requirement 3: 매출 추이 데이터 조회

**User Story:** 소상공인 사용자로서, 최근 수개월간의 매출 추이를 확인하고 싶다. 그래서 매출 변화 패턴을 파악하고 사업 전략을 수립할 수 있다.

#### Acceptance Criteria

1. WHEN 대시보드를 조회하면, THE MyBiz_Dashboard_API SHALL revenueTrend 필드에 조회 기준월 포함 이전 최대 5개월의 데이터를 반환한다.
2. THE MyBiz_Dashboard_API SHALL revenueTrend 데이터를 referenceMonth 오름차순(과거→최근)으로 정렬하여 반환한다.
3. THE MyBiz_Dashboard_API SHALL revenueTrend의 각 항목에 referenceMonth(yyyy-MM 형식, 문자열)와 monthlyRevenue(원 단위 정수, 0 이상)를 포함하여 반환한다.
4. IF 조회 기준월 이전 데이터가 5개월 미만으로 존재하면, THEN THE MyBiz_Dashboard_API SHALL 존재하는 데이터만 포함한 배열을 반환한다.
5. IF 조회 기준월 범위 내 매출 데이터가 0건이면, THEN THE MyBiz_Dashboard_API SHALL revenueTrend 필드에 빈 배열을 반환한다.

### Requirement 4: 현금흐름 추이 데이터 조회

**User Story:** 소상공인 사용자로서, 최근 수개월간의 현금흐름 추이를 확인하고 싶다. 그래서 입출금 패턴을 파악하고 자금 관리를 효율적으로 할 수 있다.

#### Acceptance Criteria

1. WHEN 대시보드를 조회하면, THE MyBiz_Dashboard_API SHALL cashFlowTrend 필드에 조회 기준월 포함 이전 최대 3개월의 데이터를 반환한다.
2. THE MyBiz_Dashboard_API SHALL cashFlowTrend 데이터를 reference_month 오름차순으로 정렬하여 반환한다.
3. THE MyBiz_Dashboard_API SHALL cashFlowTrend의 각 항목에 referenceMonth(yyyy-MM 형식), monthlyInflow(원 단위 정수), monthlyOutflow(원 단위 정수)를 포함하여 반환한다.
4. IF 조회 기준월 이전 데이터가 3개월 미만으로 존재하면, THEN THE MyBiz_Dashboard_API SHALL 존재하는 데이터만 반환한다.
5. IF 해당 사용자의 현금흐름 데이터가 존재하지 않으면, THEN THE MyBiz_Dashboard_API SHALL cashFlowTrend 필드에 빈 배열을 반환한다.

### Requirement 5: 데이터 미존재 에러 처리

**User Story:** 서비스 운영자로서, My Biz Data가 없는 사용자에게 명확한 에러를 반환하고 싶다. 그래서 사용자가 데이터 수집이 필요함을 인지할 수 있다.

#### Acceptance Criteria

1. IF 인증된 사용자의 MyBizData 레코드가 데이터베이스에 존재하지 않으면, THEN THE MyBiz_Dashboard_API SHALL HTTP 404 상태 코드와 함께 `MY_BIZ_DATA_NOT_FOUND` 에러 코드, `result: null`을 포함한 표준 ApiResponse 형식의 에러 응답을 반환한다.
2. IF month 파라미터로 지정한 기준월(yyyy-MM 형식)에 해당하는 MyBizData 레코드가 존재하지 않으면, THEN THE MyBiz_Dashboard_API SHALL HTTP 404 상태 코드와 함께 `MY_BIZ_DATA_NOT_FOUND` 에러 코드, `result: null`을 포함한 표준 ApiResponse 형식의 에러 응답을 반환한다.

### Requirement 6: 인증 검증

**User Story:** 서비스 운영자로서, 인증되지 않은 사용자의 대시보드 조회를 차단하고 싶다. 그래서 타인의 사업 데이터가 노출되지 않는다.

#### Acceptance Criteria

1. IF 세션이 존재하지 않거나 만료된 상태에서 `GET /api/my-biz/dashboard`를 요청하면, THEN THE MyBiz_Dashboard_API SHALL HTTP 401 상태 코드와 함께 `COMMON4001` 에러 코드 및 `isSuccess: false`를 포함한 공통 응답 포맷으로 반환한다.
2. THE MyBiz_Dashboard_API SHALL 세션에서 추출한 userId를 기준으로 해당 사용자의 MyBizData만 조회하며, 다른 사용자의 userId를 요청 파라미터로 전달하더라도 세션의 userId만을 사용하여 데이터를 필터링한다.

### Requirement 7: Swagger 문서화

**User Story:** 프론트엔드 개발자로서, My Biz 대시보드 조회 API의 명세를 Swagger에서 확인하고 싶다. 그래서 API 연동 시 정확한 요청/응답 형식을 파악할 수 있다.

#### Acceptance Criteria

1. THE MyBiz_Dashboard_API SHALL MyBizControllerDocs 인터페이스에 Swagger 어노테이션(@Operation, @ApiResponses, @Tag, @Parameter)을 정의하고, MyBizController가 해당 인터페이스를 implements하여 Controller 클래스에는 Swagger 어노테이션이 존재하지 않도록 분리한다.
2. THE MyBizControllerDocs SHALL @Operation 어노테이션에 API 요약(summary)과 상세 설명(description)을 포함하고, @ApiResponses에 성공 응답(200), 인증 실패(401), 데이터 미존재(404) 응답 코드와 각 코드별 설명을 명시한다.
3. THE MyBizControllerDocs SHALL @Tag 어노테이션의 name 속성에 "My Biz Data"를, description 속성에 해당 API 그룹의 기능 범위 설명을 명시한다.
4. THE MyBizControllerDocs SHALL month 쿼리 파라미터에 @Parameter 어노테이션을 사용하여 파라미터 설명, 필수 여부(required = false), 예시 값(example = "2024-05")을 명시하고, 파라미터 형식이 "yyyy-MM"임을 description에 포함한다.
5. WHEN Swagger UI에서 My Biz Data API 명세를 조회할 때, THE MyBizControllerDocs SHALL 응답 본문의 스키마가 실제 응답 DTO 클래스 구조(필드명, 타입)와 일치하도록 메서드 반환 타입을 정확히 선언한다.
