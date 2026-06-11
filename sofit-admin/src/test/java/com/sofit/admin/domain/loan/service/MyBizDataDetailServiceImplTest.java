package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.entity.mybiz.enums.InsurancePaymentStatus;
import com.sofit.common.entity.mybiz.enums.VatFilingStatus;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.mybiz.MyBizDataRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("MyBizDataDetailServiceImpl 단위 테스트")
class MyBizDataDetailServiceImplTest {

    @InjectMocks
    private MyBizDataDetailServiceImpl myBizDataDetailService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private MyBizDataRepository myBizDataRepository;

    @Nested
    @DisplayName("findMyBizDataDetail")
    class FindMyBizDataDetailTest {

        @Test
        @DisplayName("LoanApplication이 존재하지 않으면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenApplicationNotExists() {
            // given
            given(loanApplicationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> myBizDataDetailService.findMyBizDataDetail(999L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("bizDataId가 null이면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenBizDataIdIsNull() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getBizDataId()).willReturn(null);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            // when & then
            assertThatThrownBy(() -> myBizDataDetailService.findMyBizDataDetail(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("MyBizData가 존재하지 않으면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenMyBizDataNotExists() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getBizDataId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));
            given(myBizDataRepository.findById(100L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> myBizDataDetailService.findMyBizDataDetail(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("정상 조회 시 MyBizDataDetailResponse를 반환한다")
        void shouldReturnMyBizDataDetailResponse() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getBizDataId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            LocalDate baseMonth = LocalDate.of(2026, 5, 1);
            MyBizData baseData = createMockMyBizData(baseMonth, "1023456789");
            given(myBizDataRepository.findById(100L)).willReturn(Optional.of(baseData));

            MyBizData trendData1 = createTrendMockData(LocalDate.of(2025, 12, 1), 9_300_000L, 2_300_000L, 7_700_000L);
            MyBizData trendData2 = createTrendMockData(LocalDate.of(2026, 1, 1), 10_200_000L, 2_600_000L, 8_000_000L);
            given(myBizDataRepository.findByBusinessNumberAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                    "1023456789", baseMonth.minusMonths(5), baseMonth))
                    .willReturn(List.of(trendData1, trendData2, baseData));

            // when
            MyBizDataDetailResponse response = myBizDataDetailService.findMyBizDataDetail(1L);

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

            // 추이
            assertThat(response.revenueTrend()).hasSize(3);
            assertThat(response.profitTrend()).hasSize(3);
            assertThat(response.industryAvgRevenueTrend()).hasSize(3);

            // 업종/상권 비교
            assertThat(response.industryComparison().myRevenue()).isEqualTo(11_500_000L);
            assertThat(response.industryComparison().industryAvgRevenue()).isEqualTo(9_800_000L);
        }

        @Test
        @DisplayName("이전 데이터가 없으면 기준 월 데이터 1개만 반환한다")
        void shouldReturnEmptyTrendWhenNoHistoricalData() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getBizDataId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            LocalDate baseMonth = LocalDate.of(2026, 5, 1);
            MyBizData baseData = createMockMyBizData(baseMonth, "1023456789");
            given(myBizDataRepository.findById(100L)).willReturn(Optional.of(baseData));

            given(myBizDataRepository.findByBusinessNumberAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                    "1023456789", baseMonth.minusMonths(5), baseMonth))
                    .willReturn(List.of(baseData));

            // when
            MyBizDataDetailResponse response = myBizDataDetailService.findMyBizDataDetail(1L);

            // then
            assertThat(response.revenueTrend()).hasSize(1);
            assertThat(response.profitTrend()).hasSize(1);
            assertThat(response.industryAvgRevenueTrend()).hasSize(1);
        }
    }

    // --- 헬퍼 메서드 ---

    private MyBizData createMockMyBizData(LocalDate referenceMonth, String businessNumber) {
        MyBizData data = mock(MyBizData.class);
        given(data.getReferenceMonth()).willReturn(referenceMonth);
        given(data.getBusinessNumber()).willReturn(businessNumber);
        given(data.getExistingLoanCount()).willReturn(1);
        given(data.getAnnualIncome()).willReturn(132_000_000L);
        given(data.getAnnualRepayment()).willReturn(30_400_000L);
        given(data.getMonthlyRepayment()).willReturn(2_530_000L);
        given(data.getTotalLoanBalance()).willReturn(15_000_000L);
        given(data.getBusinessAgeMonths()).willReturn(18);
        given(data.getVatFilingStatus()).willReturn(VatFilingStatus.FILED);
        given(data.getVatFilingDate()).willReturn(LocalDate.of(2026, 4, 25));
        given(data.getTaxOverdue()).willReturn(false);
        given(data.getInsurancePaymentStatus()).willReturn(InsurancePaymentStatus.PAID);
        given(data.getMonthlyRevenue()).willReturn(11_500_000L);
        given(data.getEstimatedProfit()).willReturn(3_100_000L);
        given(data.getIndustryAvgRevenue()).willReturn(9_800_000L);
        given(data.getDistrictAvgRevenue()).willReturn(10_100_000L);
        given(data.getMonthlyProfitRate()).willReturn(new BigDecimal("26.96"));
        given(data.getIndustryAvgProfitRate()).willReturn(new BigDecimal("24.00"));
        given(data.getDistrictAvgProfitRate()).willReturn(new BigDecimal("23.50"));
        given(data.getIndustrySalesRank()).willReturn(new BigDecimal("32.00"));
        given(data.getIndustryProfitRank()).willReturn(new BigDecimal("35.50"));
        given(data.getIndustrySatisfactionRank()).willReturn(new BigDecimal("33.10"));
        given(data.getDistrictSalesRank()).willReturn(new BigDecimal("28.50"));
        given(data.getDistrictProfitRank()).willReturn(new BigDecimal("30.20"));
        given(data.getDistrictSatisfactionRank()).willReturn(new BigDecimal("32.80"));
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
