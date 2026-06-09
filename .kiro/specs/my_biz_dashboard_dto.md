# MyBizDashboardResponse DTO 개편 요청

## 개요
마이 비즈 데이터 대시보드 UI가 탭 구조로 변경됨에 따라 응답 DTO를 확장합니다.
호출 방식은 기존과 동일하게 **화면 진입 시 단일 API 1회 호출**이며, 탭 전환은 FE에서 처리합니다.
월 변경 시에만 재호출합니다.

---

## 기존 DTO
```java
public record MyBizDashboardResponse(
        String referenceMonth,
        Long monthlyRevenue,
        BigDecimal monthlyRevenueGrowthRate,
        Long cashFlow,
        Long estimatedProfit,
        IndustryCompareResponse industryCompare,
        List<RevenueTrendResponse> revenueTrend,
        List<CashFlowTrendResponse> cashFlowTrend,
        List<RatingTrendResponse> ratingTrend,
        BigDecimal reviewRating,
        Integer reviewCount,
        BigDecimal onlineReorderRate,
        Integer deliveryOrderCount,
        List<String> availableMonths
)
```

---

## 변경 후 DTO
```java
public record MyBizDashboardResponse(

        // ── 공통 ──────────────────────────────────────────
        String referenceMonth,               // 조회 기준 월 (YYYY-MM)
        List<String> availableMonths,        // 조회 가능한 월 목록

        // ── 1번 탭: 이번 달 장사는 어땠나요? ──────────────
        Long monthlyRevenue,                 // 이번 달 매출
        BigDecimal monthlyRevenueGrowthRate, // 전월 대비 증감률
        Long prevMonthRevenue,               // 전월 매출액 🆕
        Integer monthlyTransactionCount,     // 월 거래 건수 🆕
        BigDecimal avgTransactionAmount,     // 건당 평균 결제액 🆕
        List<RevenueTrendResponse> revenueTrend, // 최근 6개월 매출 추이

        // ── 2번 탭: 실제로 얼마나 남았나요? ──────────────
        Long estimatedProfit,                // 추정 순이익
        Long cashFlow,                       // 현금 흐름
        List<CashFlowTrendResponse> cashFlowTrend, // 최근 6개월 입출금 추이

        // ── 3번 탭: 손님들은 다시 찾아오고 있나요? ───────
        BigDecimal reviewRating,             // 평균 평점
        Integer reviewCount,                 // 리뷰 수
        List<RatingTrendResponse> ratingTrend, // 평점 추이 차트
        BigDecimal onlineReorderRate,        // 재구매율 (%)
        BigDecimal onlineReplyRate,          // 리뷰 답글 비율 (%) 🆕
        Integer onlineInfoUpdateCount,       // 정보 수정 횟수 🆕
        BigDecimal positiveReviewRatio,      // 긍정 리뷰 비율 (%) 🆕
        BigDecimal deliveryRating,           // 배달앱 평점 🆕
        Integer deliveryOrderCount,          // 배달앱 주문 수
        Long deliverySalesAmount,            // 배달앱 매출액 🆕
        Boolean hasOnlineReservation,        // 온라인 예약 여부 🆕
        Boolean hasSns,                      // SNS 운영 여부 🆕

        // ── 4번 탭: 우리 가게는 다른 가게보다 잘하고 있나요? ─
        IndustryCompareResponse industryCompare

) {

    public record IndustryCompareResponse(
            String industryName,
            BigDecimal industrySalesRank,          // 매출 순위 상위 %
            BigDecimal industryProfitRank,         // 수익성 순위 상위 %
            BigDecimal industryStabilityRank,      // 안정성 순위 상위 %
            BigDecimal industrySalesRankChange,    // 매출 순위 전월 대비 변동 (%p) 🆕 nullable
            BigDecimal industryProfitRankChange,   // 수익성 순위 전월 대비 변동 (%p) 🆕 nullable
            BigDecimal industryStabilityRankChange // 안정성 순위 전월 대비 변동 (%p) 🆕 nullable
    ) {}

    public record RevenueTrendResponse(
            String referenceMonth,
            Long monthlyRevenue
    ) {}

    public record CashFlowTrendResponse(
            String referenceMonth,
            Long monthlyInflow,
            Long monthlyOutflow
    ) {}

    public record RatingTrendResponse(
            String referenceMonth,
            BigDecimal reviewRating
    ) {}
}
```

---

## 추가된 필드 요약 (🆕 표시)

| 필드 | 타입 | 출처 (my_biz_data 컬럼) | 비고 |
|---|---|---|---|
| `prevMonthRevenue` | Long | `prev_month_revenue` | 1번 탭 전월 매출 카드 |
| `monthlyTransactionCount` | Integer | `monthly_transaction_count` | 1번 탭 거래 현황 |
| `avgTransactionAmount` | BigDecimal | `avg_transaction_amount` | 1번 탭 거래 현황 |
| `onlineReplyRate` | BigDecimal | `online_reply_rate` | 3번 탭 답글 비율 |
| `onlineInfoUpdateCount` | Integer | `online_info_update_count` | 3번 탭 정보 수정 횟수 |
| `positiveReviewRatio` | BigDecimal | `positive_review_ratio` | 3번 탭 긍정 리뷰 비율 |
| `deliveryRating` | BigDecimal | `delivery_rating` | 3번 탭 배달앱 평점 |
| `deliverySalesAmount` | Long | `delivery_sales_amount` | 3번 탭 배달앱 매출액 |
| `hasOnlineReservation` | Boolean | `has_online_reservation` | 3번 탭 온라인 예약 여부 |
| `hasSns` | Boolean | `has_sns` | 3번 탭 SNS 운영 여부 |
| `industrySalesRankChange` | BigDecimal | 계산값 (현재 월 - 직전 월 rank) | 4번 탭 nullable |
| `industryProfitRankChange` | BigDecimal | 계산값 | 4번 탭 nullable |
| `industryStabilityRankChange` | BigDecimal | 계산값 | 4번 탭 nullable |

---

## 참고 사항

- **`loanBalance`(대출 잔액)** 는 별도 API로 조회 중이므로 이 DTO에 포함하지 않음
- **`industrySalesRankChange` 등 순위 변동값** 은 DB 컬럼이 아닌 서비스 레이어에서 계산
  - 현재 월 `industry_sales_rank` - 직전 월 `industry_sales_rank`
  - 직전 월 데이터가 없으면 `null` 반환
- **배달앱 섹션** (`deliveryRating`, `deliveryOrderCount`, `deliverySalesAmount`) 은 `deliverySalesAmount = 0`이면 FE에서 섹션 숨김 처리 예정
