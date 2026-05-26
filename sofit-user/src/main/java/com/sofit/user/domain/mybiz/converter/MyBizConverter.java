package com.sofit.user.domain.mybiz.converter;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.CashFlowTrendResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.IndustryCompareResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.RatingTrendResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.RevenueTrendResponse;

public class MyBizConverter {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private MyBizConverter() {
    }

    public static MyBizDashboardResponse toMyBizDashboardResponse(
            MyBizData baseData,
            List<MyBizData> fiveMonthTrendData,
            List<MyBizData> cashFlowTrendData,
            List<MyBizData> allMonthsData) {

        String referenceMonth = baseData.getReferenceMonth().format(YEAR_MONTH_FORMATTER);

        // 전월 데이터 없으면 증감률 null (0은 "변화 없음", null은 "비교 불가" — 프론트 UI 구분)
        BigDecimal revenueGrowthRate = baseData.getPrevMonthRevenue() == null
                ? null
                : baseData.getMonthlyRevenueGrowthRate();

        IndustryCompareResponse industryCompare = new IndustryCompareResponse(
                baseData.getIndustryName(),
                baseData.getIndustrySalesRank(),
                baseData.getIndustryProfitRank(),
                baseData.getIndustryStabilityRank()
        );

        List<RevenueTrendResponse> revenueTrend = fiveMonthTrendData.stream()
                .filter(data -> data.getMonthlyRevenue() != null)
                .map(data -> new RevenueTrendResponse(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getMonthlyRevenue()
                ))
                .toList();

        List<CashFlowTrendResponse> cashFlowTrend = cashFlowTrendData.stream()
                .filter(data -> data.getMonthlyInflow() != null && data.getMonthlyOutflow() != null)
                .map(data -> new CashFlowTrendResponse(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getMonthlyInflow(),
                        data.getMonthlyOutflow()
                ))
                .toList();

        List<RatingTrendResponse> ratingTrend = fiveMonthTrendData.stream()
                .filter(data -> data.getReviewRating() != null)
                .map(data -> new RatingTrendResponse(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getReviewRating()
                ))
                .toList();

        List<String> availableMonths = allMonthsData.stream()
                .map(data -> data.getReferenceMonth().format(YEAR_MONTH_FORMATTER))
                .toList();

        return new MyBizDashboardResponse(
                referenceMonth,
                baseData.getMonthlyRevenue(),
                revenueGrowthRate,
                baseData.getCashFlow(),
                baseData.getEstimatedProfit(),
                industryCompare,
                revenueTrend,
                cashFlowTrend,
                ratingTrend,
                baseData.getReviewRating(),
                baseData.getReviewCount(),
                baseData.getOnlineReorderRate(),
                baseData.getDeliveryOrderCount(),
                availableMonths
        );
    }
}
