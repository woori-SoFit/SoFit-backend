package com.sofit.admin.domain.loan.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record MyBizDataDetailResponse(
        // 데이터 기준 시점 (스냅샷 기준월)
        String dataAsOf,

        // 기본 재무/DSR
        Integer existingLoanCount,
        Long annualIncome,
        Long annualRepayment,
        Long monthlyRepayment,
        Long totalLoanBalance,

        // 운영 신뢰도
        Integer businessAgeMonths,
        String vatFilingStatus,
        String vatFilingDate,
        Boolean taxOverdue,
        String insurancePaymentStatus,

        // 추이 (최근 6개월)
        List<RevenueTrendItem> revenueTrend,
        List<ProfitTrendItem> profitTrend,
        List<IndustryAvgRevenueTrendItem> industryAvgRevenueTrend,

        // 업종/상권 비교
        IndustryComparisonResponse industryComparison
) {

    public record RevenueTrendItem(
            String referenceMonth,
            Long monthlyRevenue
    ) {}

    public record ProfitTrendItem(
            String referenceMonth,
            Long profit
    ) {}

    public record IndustryAvgRevenueTrendItem(
            String referenceMonth,
            Long monthlyRevenue
    ) {}

    public record IndustryComparisonResponse(
            Long myRevenue,
            Long industryAvgRevenue,
            Long districtAvgRevenue,
            BigDecimal myProfitRate,
            BigDecimal industryAvgProfitRate,
            BigDecimal districtAvgProfitRate,
            BigDecimal industrySalesRank,
            BigDecimal industryProfitRank,
            BigDecimal industrySatisfactionRank,
            BigDecimal districtSalesRank,
            BigDecimal districtProfitRank,
            BigDecimal districtSatisfactionRank
    ) {}
}
