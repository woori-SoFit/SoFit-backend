package com.sofit.user.domain.mybiz.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MyBizDashboardResponse(
        // === 공통 ===
        String referenceMonth,
        List<String> availableMonths,

        // === 1번 탭: 매출 ===
        Long monthlyRevenue,
        BigDecimal monthlyRevenueGrowthRate,
        Long prevMonthRevenue,
        Integer monthlyTransactionCount,
        BigDecimal avgTransactionAmount,
        List<RevenueTrendResponse> revenueTrend,

        // === 2번 탭: 수익/현금흐름 ===
        Long cashFlow,
        Long estimatedProfit,
        List<CashFlowTrendResponse> cashFlowTrend,

        // === 3번 탭: 고객/온라인 ===
        BigDecimal reviewRating,
        Integer reviewCount,
        BigDecimal onlineReorderRate,
        Integer deliveryOrderCount,
        BigDecimal onlineReplyRate,
        Integer onlineInfoUpdateCount,
        BigDecimal positiveReviewRatio,
        BigDecimal deliveryRating,
        Long deliverySalesAmount,
        Boolean hasOnlineReservation,
        Boolean hasSns,
        List<RatingTrendResponse> ratingTrend,

        // === 4번 탭: 업종 비교 ===
        IndustryCompareResponse industryCompare
) {

    public record IndustryCompareResponse(
            String industryName,
            BigDecimal industrySalesRank,
            BigDecimal industryProfitRank,
            BigDecimal industryStabilityRank,
            BigDecimal industrySalesRankChange,
            BigDecimal industryProfitRankChange,
            BigDecimal industryStabilityRankChange
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
