package com.sofit.user.domain.mybiz.dto.response;

import java.math.BigDecimal;
import java.util.List;

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
        BigDecimal naverRating,
        Integer reviewCount,
        BigDecimal deliveryReorderRate,
        Integer deliveryOrderCount
) {

    public record IndustryCompareResponse(
            String industryName,
            BigDecimal industrySalesRank,
            BigDecimal industryProfitRank,
            BigDecimal industryStabilityRank
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
