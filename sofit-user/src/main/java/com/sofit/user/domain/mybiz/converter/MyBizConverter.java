package com.sofit.user.domain.mybiz.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.PaymentFlowTrendResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse.RevenueTrendResponse;

public class MyBizConverter {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private MyBizConverter() {
    }

    public static MyBizDashboardResponse toMyBizDashboardResponse(
            MyBizData baseData,
            List<MyBizData> sixMonthTrendData,
            List<LocalDate> availableMonths,
            BigDecimal monthlyRevenueGrowthRate) {

        String referenceMonth = baseData.getReferenceMonth().format(YEAR_MONTH_FORMATTER);

        // revenueTrend: monthlyRevenue가 non-null인 것만 매핑
        List<RevenueTrendResponse> revenueTrend = sixMonthTrendData.stream()
                .filter(data -> data.getMonthlyRevenue() != null)
                .map(data -> new RevenueTrendResponse(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getMonthlyRevenue()
                ))
                .toList();

        // paymentFlowTrend: monthlyRevenue, monthlyOutflow, estimatedProfit 매핑
        List<PaymentFlowTrendResponse> paymentFlowTrend = sixMonthTrendData.stream()
                .map(data -> new PaymentFlowTrendResponse(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getMonthlyRevenue(),
                        data.getMonthlyOutflow(),
                        data.getEstimatedProfit()
                ))
                .toList();

        List<String> availableMonthStrings = availableMonths.stream()
                .map(date -> date.format(YEAR_MONTH_FORMATTER))
                .toList();

        return new MyBizDashboardResponse(
                // 공통
                referenceMonth,
                availableMonthStrings,
                // 1번 탭: 매출
                baseData.getMonthlyRevenue(),
                baseData.getPosSalesAmount(),
                baseData.getDeliverySalesAmount(),
                monthlyRevenueGrowthRate,
                baseData.getMonthlyPaymentCount(),
                baseData.getAvgPaymentAmount(),
                revenueTrend,
                baseData.getAvgRevenueMon(),
                baseData.getAvgRevenueTue(),
                baseData.getAvgRevenueWed(),
                baseData.getAvgRevenueThu(),
                baseData.getAvgRevenueFri(),
                baseData.getAvgRevenueSat(),
                baseData.getAvgRevenueSun(),
                // 2번 탭: 수익
                baseData.getEstimatedProfit(),
                baseData.getMonthlyOutflow(),
                paymentFlowTrend,
                baseData.getMonthlyProfitGrowthRate(),
                // 3번 탭: 고객/온라인
                baseData.getReviewRating(),
                baseData.getReviewCount(),
                baseData.getPositiveReviewRatio(),
                baseData.getNegativeReviewRatio(),
                baseData.getDeliveryRating(),
                baseData.getHasOnlineReservation(),
                baseData.getHasSns(),
                baseData.getOnlineReplyRate(),
                // 4번 탭: 업종/상권 비교
                baseData.getIndustryName(),
                baseData.getIndustrySalesRank(),
                baseData.getIndustryProfitRank(),
                baseData.getIndustrySatisfactionRank(),
                baseData.getDistrictSalesRank(),
                baseData.getDistrictProfitRank(),
                baseData.getDistrictSatisfactionRank(),
                baseData.getMonthlyProfitRate(),
                baseData.getIndustryAvgRevenue(),
                baseData.getIndustryAvgProfitRate(),
                baseData.getIndustryAvgReviewRating(),
                baseData.getDistrictAvgRevenue(),
                baseData.getDistrictAvgProfitRate(),
                baseData.getDistrictAvgReviewRating()
        );
    }

    /**
     * 매출 전월대비 증감률 계산.
     * prevMonthRevenue가 null이거나 0이면 null 반환.
     */
    public static BigDecimal calculateMonthlyRevenueGrowthRate(Long monthlyRevenue, Long prevMonthRevenue) {
        if (prevMonthRevenue == null || prevMonthRevenue == 0 || monthlyRevenue == null) {
            return null;
        }
        return BigDecimal.valueOf(monthlyRevenue - prevMonthRevenue)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(prevMonthRevenue), 2, RoundingMode.HALF_UP);
    }
}
