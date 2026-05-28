package com.sofit.admin.domain.loan.dto.response;

import java.math.BigDecimal;

public record MyBizDataDetailResponse(
        Long annualIncome,
        Integer existingLoanCount,
        Long monthlyRevenue,
        BigDecimal monthlyRevenueGrowthRate,
        Long cashFlow,
        Long accountBalance,
        Integer businessAgeMonths,
        String vatFilingStatus,
        Boolean taxOverdue,
        String insurancePaymentStatus,
        BigDecimal industrySalesRank,
        BigDecimal industryProfitRank
) {}
