package com.sofit.admin.domain.loan.converter;

import java.time.format.DateTimeFormatter;
import java.util.List;

import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse.IndustryAvgRevenueTrendItem;
import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse.IndustryComparisonResponse;
import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse.ProfitTrendItem;
import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse.RevenueTrendItem;
import com.sofit.common.entity.mybiz.MyBizData;

public class MyBizDataDetailConverter {

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    private MyBizDataDetailConverter() {}

    public static MyBizDataDetailResponse toMyBizDataDetailResponse(
            MyBizData baseData, List<MyBizData> sixMonthTrendData) {

        // 추이 데이터 변환
        List<RevenueTrendItem> revenueTrend = sixMonthTrendData.stream()
                .filter(data -> data.getReferenceMonth() != null && data.getMonthlyRevenue() != null)
                .map(data -> new RevenueTrendItem(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getMonthlyRevenue()))
                .toList();

        List<ProfitTrendItem> profitTrend = sixMonthTrendData.stream()
                .filter(data -> data.getReferenceMonth() != null && data.getEstimatedProfit() != null)
                .map(data -> new ProfitTrendItem(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getEstimatedProfit()))
                .toList();

        List<IndustryAvgRevenueTrendItem> industryAvgRevenueTrend = sixMonthTrendData.stream()
                .filter(data -> data.getReferenceMonth() != null && data.getIndustryAvgRevenue() != null)
                .map(data -> new IndustryAvgRevenueTrendItem(
                        data.getReferenceMonth().format(YEAR_MONTH_FORMATTER),
                        data.getIndustryAvgRevenue()))
                .toList();

        // 업종/상권 비교 (최신 월 기준)
        IndustryComparisonResponse industryComparison = new IndustryComparisonResponse(
                baseData.getMonthlyRevenue(),
                baseData.getIndustryAvgRevenue(),
                baseData.getDistrictAvgRevenue(),
                baseData.getMonthlyProfitRate(),
                baseData.getIndustryAvgProfitRate(),
                baseData.getDistrictAvgProfitRate(),
                baseData.getIndustrySalesRank(),
                baseData.getIndustryProfitRank(),
                baseData.getIndustrySatisfactionRank(),
                baseData.getDistrictSalesRank(),
                baseData.getDistrictProfitRank(),
                baseData.getDistrictSatisfactionRank());

        return new MyBizDataDetailResponse(
                baseData.getReferenceMonth() != null
                        ? baseData.getReferenceMonth().format(YEAR_MONTH_FORMATTER) : null,
                baseData.getExistingLoanCount(),
                baseData.getAnnualIncome(),
                baseData.getAnnualRepayment(),
                baseData.getMonthlyRepayment(),
                baseData.getTotalLoanBalance(),
                baseData.getBusinessAgeMonths(),
                baseData.getVatFilingStatus() != null ? baseData.getVatFilingStatus().name() : null,
                baseData.getVatFilingDate() != null ? baseData.getVatFilingDate().toString() : null,
                baseData.getTaxOverdue(),
                baseData.getInsurancePaymentStatus() != null ? baseData.getInsurancePaymentStatus().name() : null,
                revenueTrend,
                profitTrend,
                industryAvgRevenueTrend,
                industryComparison);
    }
}
