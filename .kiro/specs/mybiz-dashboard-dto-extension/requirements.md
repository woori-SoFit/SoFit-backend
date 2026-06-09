# Requirements Document

## Introduction

마이 비즈 데이터 대시보드 UI가 4개 탭 구조로 변경됨에 따라, 기존 단일 API 응답 DTO(MyBizDashboardResponse)를 확장한다.
호출 방식은 기존과 동일하게 화면 진입 시 단일 API 1회 호출이며, 탭 전환은 프론트엔드에서 처리한다.
월 변경 시에만 재호출한다.

## Glossary

- **Dashboard_API**: GET /api/mybiz/dashboard 엔드포인트. 마이 비즈 데이터 대시보드 응답을 반환하는 API
- **MyBizDashboardResponse**: Dashboard_API의 응답 DTO(record). 4개 탭에 필요한 모든 데이터를 단일 객체로 포함
- **IndustryCompareResponse**: MyBizDashboardResponse 내부에 중첩된 record. 업종 비교 순위 정보를 담는 DTO
- **MyBizData**: my_biz_data 테이블에 매핑된 JPA 엔티티. 소상공인 사업 데이터의 원천
- **MyBizConverter**: MyBizData 엔티티를 MyBizDashboardResponse DTO로 변환하는 Converter 클래스
- **MyBizServiceImpl**: 대시보드 조회 비즈니스 로직을 처리하는 서비스 구현체
- **referenceMonth**: 조회 기준 월 (yyyy-MM 형식). MyBizData의 reference_month 컬럼에 대응
- **rankChange**: 업종 순위 전월 대비 변동값. 현재 월 rank에서 직전 월 rank를 뺀 계산값 (%p 단위)

## Requirements

### Requirement 1: MyBizDashboardResponse 필드 확장

**User Story:** As a 소상공인 사용자, I want 대시보드에서 탭별로 구분된 상세 사업 데이터를 확인하고 싶다, so that 매출, 수익, 고객 재방문, 업종 비교를 한눈에 파악할 수 있다.

#### Acceptance Criteria

1. THE MyBizDashboardResponse SHALL 기존 필드(referenceMonth, monthlyRevenue, monthlyRevenueGrowthRate, cashFlow, estimatedProfit, industryCompare, revenueTrend, cashFlowTrend, ratingTrend, reviewRating, reviewCount, onlineReorderRate, deliveryOrderCount, availableMonths)를 유지한다
2. THE MyBizDashboardResponse SHALL 1번 탭용 신규 필드로 prevMonthRevenue(Long, nullable), monthlyTransactionCount(Integer, 0 이상), avgTransactionAmount(BigDecimal, 0 이상)를 포함한다
3. THE MyBizDashboardResponse SHALL 3번 탭용 신규 필드로 onlineReplyRate(BigDecimal, 0.00~100.00), onlineInfoUpdateCount(Integer, 0 이상), positiveReviewRatio(BigDecimal, 0.00~100.00), deliveryRating(BigDecimal, 0.0~5.0, nullable), deliverySalesAmount(Long, 0 이상), hasOnlineReservation(Boolean), hasSns(Boolean)를 포함한다
4. THE MyBizDashboardResponse SHALL 4번 탭용 IndustryCompareResponse에 신규 필드로 industrySalesRankChange(BigDecimal, nullable), industryProfitRankChange(BigDecimal, nullable), industryStabilityRankChange(BigDecimal, nullable)를 추가한다
5. THE MyBizDashboardResponse SHALL 필드 순서를 공통(referenceMonth, availableMonths), 1번 탭, 2번 탭, 3번 탭, 4번 탭 순으로 배치한다
6. IF 직전 월 데이터가 존재하지 않는 경우, THEN THE MyBizDashboardResponse SHALL industrySalesRankChange, industryProfitRankChange, industryStabilityRankChange 필드를 null로 반환한다

### Requirement 2: IndustryCompareResponse 순위 변동 필드 추가

**User Story:** As a 소상공인 사용자, I want 업종 순위의 전월 대비 변동을 확인하고 싶다, so that 경쟁력 변화 추이를 파악할 수 있다.

#### Acceptance Criteria

1. THE IndustryCompareResponse SHALL industrySalesRankChange(BigDecimal, nullable), industryProfitRankChange(BigDecimal, nullable), industryStabilityRankChange(BigDecimal, nullable) 필드를 포함한다
2. WHEN 기준월(referenceMonth)의 직전 월(referenceMonth에서 1개월 전) MyBizData가 존재하면, THE MyBizServiceImpl SHALL 현재 월 rank 값에서 직전 월 rank 값을 빼서 rankChange 값을 계산한다 (양수: 순위 수치 상승, 음수: 순위 수치 하락)
3. IF 직전 월(referenceMonth에서 1개월 전) MyBizData가 존재하지 않으면, THEN THE MyBizServiceImpl SHALL 해당 rankChange 값을 null로 반환한다
4. IF 직전 월 MyBizData는 존재하지만 현재 월 또는 직전 월의 개별 rank 필드(industrySalesRank, industryProfitRank, industryStabilityRank) 중 하나가 null이면, THEN THE MyBizServiceImpl SHALL 해당 rankChange 필드를 null로 반환한다

### Requirement 3: MyBizConverter 신규 필드 매핑

**User Story:** As a 개발자, I want Converter에서 신규 필드를 올바르게 매핑하고 싶다, so that MyBizData 엔티티의 값이 응답 DTO에 정확하게 전달된다.

#### Acceptance Criteria

1. WHEN MyBizDashboardResponse를 생성할 때, THE MyBizConverter SHALL MyBizData 엔티티의 prevMonthRevenue(Long), monthlyTransactionCount(Integer), avgTransactionAmount(BigDecimal) 값을 MyBizDashboardResponse의 동일 이름 필드에 1:1로 매핑하며, 엔티티 값이 null인 경우 DTO 필드도 null로 설정한다
2. WHEN MyBizDashboardResponse를 생성할 때, THE MyBizConverter SHALL MyBizData 엔티티의 onlineReplyRate(BigDecimal), onlineInfoUpdateCount(Integer), positiveReviewRatio(BigDecimal), deliveryRating(BigDecimal), deliverySalesAmount(Long), hasOnlineReservation(Boolean), hasSns(Boolean) 값을 MyBizDashboardResponse의 동일 이름 필드에 1:1로 매핑하며, 엔티티 값이 null인 경우 DTO 필드도 null로 설정한다
3. WHEN IndustryCompareResponse를 생성할 때, THE MyBizConverter SHALL rankChange 파라미터(BigDecimal, nullable)를 받아 IndustryCompareResponse의 industrySalesRankChange, industryProfitRankChange, industryStabilityRankChange 필드에 매핑하며, 전월 비교 데이터가 없어 계산이 불가능한 경우 null을 전달한다

### Requirement 4: 직전 월 데이터 조회 로직

**User Story:** As a 개발자, I want 서비스 레이어에서 직전 월 데이터를 조회하고 싶다, so that 순위 변동값을 정확하게 계산할 수 있다.

#### Acceptance Criteria

1. WHEN Dashboard API가 호출되면, THE MyBizServiceImpl SHALL 기준월(referenceMonth)에서 1개월을 뺀 직전 월의 MyBizData를 동일 userId 조건으로 추가 조회한다
2. WHEN 직전 월 MyBizData가 존재하면, THE MyBizServiceImpl SHALL 각 rankChange를 (기준월 rank - 직전 월 rank) 공식으로 계산하여 industrySalesRankChange, industryProfitRankChange, industryStabilityRankChange 3개 필드에 BigDecimal 값을 설정한다
3. IF 직전 월 MyBizData 조회 결과가 없으면, THEN THE MyBizServiceImpl SHALL industrySalesRankChange, industryProfitRankChange, industryStabilityRankChange 3개 필드를 모두 null로 설정하고 나머지 대시보드 데이터는 정상 반환한다

### Requirement 5: API 호환성 유지

**User Story:** As a 프론트엔드 개발자, I want 기존 API 엔드포인트와 호출 방식이 변경되지 않기를 원한다, so that 기존 코드 변경 없이 신규 필드를 활용할 수 있다.

#### Acceptance Criteria

1. THE Dashboard_API SHALL 기존 엔드포인트(GET /api/mybiz/dashboard)의 HTTP 메서드와 경로를 변경하지 않는다
2. THE Dashboard_API SHALL 기존 요청 파라미터(month, optional, String 타입)의 이름, 타입, 필수 여부를 변경하지 않는다
3. THE Dashboard_API SHALL 기존 응답 필드(referenceMonth, monthlyRevenue, monthlyRevenueGrowthRate, cashFlow, estimatedProfit, industryCompare, revenueTrend, cashFlowTrend, ratingTrend, reviewRating, reviewCount, onlineReorderRate, deliveryOrderCount, availableMonths)의 필드명, 타입, 값 산출 로직을 변경하지 않는다
4. IF 신규 필드에 대응하는 MyBizData 컬럼 값이 null이면, THEN THE MyBizDashboardResponse SHALL 해당 필드를 JSON 값 null로 반환한다
5. IF 신규 필드가 서비스 레이어 계산값(industrySalesRankChange, industryProfitRankChange, industryStabilityRankChange)이고 계산에 필요한 직전 월 데이터가 존재하지 않으면, THEN THE MyBizDashboardResponse SHALL 해당 필드를 null로 반환한다
6. THE Dashboard_API SHALL 응답 래퍼 구조(isSuccess, code, message, result)를 유지하며, 신규 필드는 기존 result 객체 내에 추가한다
