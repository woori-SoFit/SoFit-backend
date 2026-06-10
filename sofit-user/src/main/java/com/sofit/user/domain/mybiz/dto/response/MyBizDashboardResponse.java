package com.sofit.user.domain.mybiz.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MyBizDashboardResponse(
        // === 공통 ===
        String referenceMonth,
        List<String> availableMonths,

        // === 1번 탭: 매출 ===
        Long monthlyRevenue,
        Long posSalesAmount,
        Long deliverySalesAmount,
        BigDecimal monthlyRevenueGrowthRate,
        Integer monthlyPaymentCount,
        BigDecimal avgPaymentAmount,
        List<RevenueTrendResponse> revenueTrend,
        Long avgRevenueMon,
        Long avgRevenueTue,
        Long avgRevenueWed,
        Long avgRevenueThu,
        Long avgRevenueFri,
        Long avgRevenueSat,
        Long avgRevenueSun,

        // === 2번 탭: 수익 ===
        Long estimatedProfit,
        Long monthlyOutflow,
        List<PaymentFlowTrendResponse> paymentFlowTrend,
        BigDecimal monthlyProfitGrowthRate,

        // === 3번 탭: 고객/온라인 ===
        BigDecimal reviewRating,
        Integer reviewCount,
        BigDecimal positiveReviewRatio,
        BigDecimal negativeReviewRatio,
        BigDecimal deliveryRating,
        Boolean hasOnlineReservation,
        Boolean hasSns,
        BigDecimal onlineReplyRate,

        // === 4번 탭: 업종/상권 비교 ===
        String industryName,
        BigDecimal industrySalesRank,
        BigDecimal industryProfitRank,
        BigDecimal industrySatisfactionRank,
        BigDecimal districtSalesRank,
        BigDecimal districtProfitRank,
        BigDecimal districtSatisfactionRank,
        BigDecimal monthlyProfitRate,
        Long industryAvgRevenue,
        BigDecimal industryAvgProfitRate,
        BigDecimal industryAvgReviewRating,
        Long districtAvgRevenue,
        BigDecimal districtAvgProfitRate,
        BigDecimal districtAvgReviewRating
) {

    public record RevenueTrendResponse(
            String referenceMonth,
            Long monthlyRevenue
    ) {}

    public record PaymentFlowTrendResponse(
            String referenceMonth,
            Long monthlyRevenue,
            Long monthlyOutflow,
            Long estimatedProfit
    ) {}
}
