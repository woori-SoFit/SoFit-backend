package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.entity.mybiz.enums.InsurancePaymentStatus;
import com.sofit.common.entity.mybiz.enums.VatFilingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("MyBizDataDetailConverter 단위 테스트")
class MyBizDataDetailConverterTest {

    @Test
    @DisplayName("정상 데이터로 MyBizDataDetailResponse를 생성한다")
    void shouldConvertToResponse() {
        // given
        MyBizData baseData = createMockMyBizData(
                LocalDate.of(2026, 5, 1), 1, 132_000_000L, 30_400_000L, 2_530_000L, 15_000_000L,
                18, VatFilingStatus.FILED, LocalDate.of(2026, 4, 25), false, InsurancePaymentStatus.PAID,
                11_500_000L, 3_100_000L, 9_200_000L, 9_800_000L,
                new BigDecimal("27.00"), new BigDecimal("19.80"), new BigDecimal("21.30"),
                new BigDecimal("8.20"), new BigDecimal("12.50"), new BigDecimal("15.30"),
                new BigDecimal("6.80"), new BigDecimal("10.20"), new BigDecimal("11.70"));

        MyBizData trendData1 = createTrendMockData(LocalDate.of(2025, 12, 1), 9_300_000L, 2_300_000L, 7_700_000L);
        MyBizData trendData2 = createTrendMockData(LocalDate.of(2026, 1, 1), 10_200_000L, 2_600_000L, 8_000_000L);

        List<MyBizData> sixMonthTrendData = List.of(trendData1, trendData2, baseData);

        // when
        MyBizDataDetailResponse response = MyBizDataDetailConverter.toMyBizDataDetailResponse(baseData, sixMonthTrendData);

        // then
        assertThat(response.dataAsOf()).isEqualTo("2026-05");
        assertThat(response.existingLoanCount()).isEqualTo(1);
        assertThat(response.annualIncome()).isEqualTo(132_000_000L);
        assertThat(response.annualRepayment()).isEqualTo(30_400_000L);
        assertThat(response.monthlyRepayment()).isEqualTo(2_530_000L);
        assertThat(response.totalLoanBalance()).isEqualTo(15_000_000L);
        assertThat(response.businessAgeMonths()).isEqualTo(18);
        assertThat(response.vatFilingStatus()).isEqualTo("FILED");
        assertThat(response.vatFilingDate()).isEqualTo("2026-04-25");
        assertThat(response.taxOverdue()).isFalse();
        assertThat(response.insurancePaymentStatus()).isEqualTo("PAID");

        // 추이 데이터
        assertThat(response.revenueTrend()).hasSize(3);
        assertThat(response.revenueTrend().get(0).referenceMonth()).isEqualTo("2025-12");
        assertThat(response.revenueTrend().get(0).monthlyRevenue()).isEqualTo(9_300_000L);

        assertThat(response.profitTrend()).hasSize(3);
        assertThat(response.profitTrend().get(0).profit()).isEqualTo(2_300_000L);

        assertThat(response.industryAvgRevenueTrend()).hasSize(3);
        assertThat(response.industryAvgRevenueTrend().get(0).monthlyRevenue()).isEqualTo(7_700_000L);

        // 업종/상권 비교
        assertThat(response.industryComparison().myRevenue()).isEqualTo(11_500_000L);
        assertThat(response.industryComparison().industryAvgRevenue()).isEqualTo(9_200_000L);
        assertThat(response.industryComparison().districtAvgRevenue()).isEqualTo(9_800_000L);
        assertThat(response.industryComparison().industrySalesRank()).isEqualByComparingTo(new BigDecimal("8.20"));
    }

    @Test
    @DisplayName("null 필드가 있어도 예외 없이 변환된다")
    void shouldHandleNullFields() {
        // given
        MyBizData baseData = mock(MyBizData.class);
        given(baseData.getReferenceMonth()).willReturn(LocalDate.of(2026, 5, 1));
        given(baseData.getExistingLoanCount()).willReturn(null);
        given(baseData.getAnnualIncome()).willReturn(null);
        given(baseData.getAnnualRepayment()).willReturn(null);
        given(baseData.getMonthlyRepayment()).willReturn(null);
        given(baseData.getTotalLoanBalance()).willReturn(null);
        given(baseData.getBusinessAgeMonths()).willReturn(null);
        given(baseData.getVatFilingStatus()).willReturn(null);
        given(baseData.getVatFilingDate()).willReturn(null);
        given(baseData.getTaxOverdue()).willReturn(null);
        given(baseData.getInsurancePaymentStatus()).willReturn(null);
        given(baseData.getMonthlyRevenue()).willReturn(null);
        given(baseData.getEstimatedProfit()).willReturn(null);
        given(baseData.getIndustryAvgRevenue()).willReturn(null);
        given(baseData.getDistrictAvgRevenue()).willReturn(null);
        given(baseData.getMonthlyProfitRate()).willReturn(null);
        given(baseData.getIndustryAvgProfitRate()).willReturn(null);
        given(baseData.getDistrictAvgProfitRate()).willReturn(null);
        given(baseData.getIndustrySalesRank()).willReturn(null);
        given(baseData.getIndustryProfitRank()).willReturn(null);
        given(baseData.getIndustrySatisfactionRank()).willReturn(null);
        given(baseData.getDistrictSalesRank()).willReturn(null);
        given(baseData.getDistrictProfitRank()).willReturn(null);
        given(baseData.getDistrictSatisfactionRank()).willReturn(null);

        // when
        MyBizDataDetailResponse response = MyBizDataDetailConverter.toMyBizDataDetailResponse(baseData, List.of());

        // then
        assertThat(response.dataAsOf()).isEqualTo("2026-05");
        assertThat(response.vatFilingStatus()).isNull();
        assertThat(response.vatFilingDate()).isNull();
        assertThat(response.insurancePaymentStatus()).isNull();
        assertThat(response.existingLoanCount()).isNull();
        assertThat(response.revenueTrend()).isEmpty();
        assertThat(response.profitTrend()).isEmpty();
        assertThat(response.industryAvgRevenueTrend()).isEmpty();
    }

    // --- 헬퍼 메서드 ---

    private MyBizData createMockMyBizData(
            LocalDate referenceMonth, Integer existingLoanCount, Long annualIncome,
            Long annualRepayment, Long monthlyRepayment, Long totalLoanBalance,
            Integer businessAgeMonths, VatFilingStatus vatFilingStatus, LocalDate vatFilingDate,
            Boolean taxOverdue, InsurancePaymentStatus insurancePaymentStatus,
            Long monthlyRevenue, Long estimatedProfit, Long industryAvgRevenue, Long districtAvgRevenue,
            BigDecimal monthlyProfitRate, BigDecimal industryAvgProfitRate, BigDecimal districtAvgProfitRate,
            BigDecimal industrySalesRank, BigDecimal industryProfitRank, BigDecimal industrySatisfactionRank,
            BigDecimal districtSalesRank, BigDecimal districtProfitRank, BigDecimal districtSatisfactionRank) {

        MyBizData data = mock(MyBizData.class);
        given(data.getReferenceMonth()).willReturn(referenceMonth);
        given(data.getExistingLoanCount()).willReturn(existingLoanCount);
        given(data.getAnnualIncome()).willReturn(annualIncome);
        given(data.getAnnualRepayment()).willReturn(annualRepayment);
        given(data.getMonthlyRepayment()).willReturn(monthlyRepayment);
        given(data.getTotalLoanBalance()).willReturn(totalLoanBalance);
        given(data.getBusinessAgeMonths()).willReturn(businessAgeMonths);
        given(data.getVatFilingStatus()).willReturn(vatFilingStatus);
        given(data.getVatFilingDate()).willReturn(vatFilingDate);
        given(data.getTaxOverdue()).willReturn(taxOverdue);
        given(data.getInsurancePaymentStatus()).willReturn(insurancePaymentStatus);
        given(data.getMonthlyRevenue()).willReturn(monthlyRevenue);
        given(data.getEstimatedProfit()).willReturn(estimatedProfit);
        given(data.getIndustryAvgRevenue()).willReturn(industryAvgRevenue);
        given(data.getDistrictAvgRevenue()).willReturn(districtAvgRevenue);
        given(data.getMonthlyProfitRate()).willReturn(monthlyProfitRate);
        given(data.getIndustryAvgProfitRate()).willReturn(industryAvgProfitRate);
        given(data.getDistrictAvgProfitRate()).willReturn(districtAvgProfitRate);
        given(data.getIndustrySalesRank()).willReturn(industrySalesRank);
        given(data.getIndustryProfitRank()).willReturn(industryProfitRank);
        given(data.getIndustrySatisfactionRank()).willReturn(industrySatisfactionRank);
        given(data.getDistrictSalesRank()).willReturn(districtSalesRank);
        given(data.getDistrictProfitRank()).willReturn(districtProfitRank);
        given(data.getDistrictSatisfactionRank()).willReturn(districtSatisfactionRank);
        return data;
    }

    private MyBizData createTrendMockData(LocalDate referenceMonth, Long monthlyRevenue,
                                           Long estimatedProfit, Long industryAvgRevenue) {
        MyBizData data = mock(MyBizData.class);
        given(data.getReferenceMonth()).willReturn(referenceMonth);
        given(data.getMonthlyRevenue()).willReturn(monthlyRevenue);
        given(data.getEstimatedProfit()).willReturn(estimatedProfit);
        given(data.getIndustryAvgRevenue()).willReturn(industryAvgRevenue);
        return data;
    }
}
