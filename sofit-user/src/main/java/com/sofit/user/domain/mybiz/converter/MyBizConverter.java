package com.sofit.user.domain.mybiz.converter;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.CashFlowTrendResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.IndustryCompareResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.RevenueTrendResponse;

public class MyBizConverter {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private MyBizConverter() {
    }

    public static MyBizDashboardResponse toMyBizDashboardResponse(
            MyBizData baseData,
            List<MyBizData> revenueTrendData,
            List<MyBizData> cashFlowTrendData) {

        String referenceMonth = baseData.getReferenceMonth().format(YEAR_MONTH_FORMATTER);

        IndustryCompareResponse industryCompare = new IndustryCompareResponse(
                baseData.getIndustrySalesRank(),
                baseData.getIndustryProfitRank(),
                baseData.getIndustryStabilityRank()
        );

        List<RevenueTrendResponse> revenueTrend = revenueTrendData.stream()
                .map(data -> new RevenueTrendResponse(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getMonthlyRevenue()
                ))
                .toList();

        List<CashFlowTrendResponse> cashFlowTrend = cashFlowTrendData.stream()
                .map(data -> new CashFlowTrendResponse(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getMonthlyInflow(),
                        data.getMonthlyOutflow()
                ))
                .toList();

        return new MyBizDashboardResponse(
                referenceMonth,
                baseData.getMonthlyRevenue(),
                baseData.getMonthlyRevenueGrowthRate(),
                baseData.getCashFlow(),
                baseData.getEstimatedProfit(),
                industryCompare,
                revenueTrend,
                cashFlowTrend,
                baseData.getReviewRating(),
                baseData.getReviewCount(),
                baseData.getOnlineReorderRate(),
                baseData.getDeliveryOrderCount()
        );
    }
}
