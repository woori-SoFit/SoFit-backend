package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.entity.mybiz.enums.InsurancePaymentStatus;
import com.sofit.common.entity.mybiz.enums.VatFilingStatus;
import com.sofit.common.entity.user.User;
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
            User user = mock(User.class);
            given(user.getUserId()).willReturn(1L);

            LoanApplication app = mock(LoanApplication.class);
            given(app.getBizDataId()).willReturn(100L);
            given(app.getUser()).willReturn(user);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            MyBizData myBizData = mock(MyBizData.class);
            given(myBizData.getAnnualIncome()).willReturn(60_000_000L);
            given(myBizData.getMonthlyRevenue()).willReturn(5_000_000L);
            given(myBizData.getMonthlyProfitGrowthRate()).willReturn(new BigDecimal("12.50"));
            given(myBizData.getBusinessAgeMonths()).willReturn(48);
            given(myBizData.getVatFilingStatus()).willReturn(VatFilingStatus.FILED);
            given(myBizData.getTaxOverdue()).willReturn(false);
            given(myBizData.getInsurancePaymentStatus()).willReturn(InsurancePaymentStatus.PAID);
            given(myBizData.getIndustrySalesRank()).willReturn(new BigDecimal("25.00"));
            given(myBizData.getIndustryProfitRank()).willReturn(new BigDecimal("30.00"));
            given(myBizDataRepository.findById(100L)).willReturn(Optional.of(myBizData));

            given(loanApplicationRepository.countByUser_UserIdAndStatus(1L, ApplicationStatus.EXECUTED))
                    .willReturn(2);

            // when
            MyBizDataDetailResponse response = myBizDataDetailService.findMyBizDataDetail(1L);

            // then
            assertThat(response.annualIncome()).isEqualTo(60_000_000L);
            assertThat(response.existingLoanCount()).isEqualTo(2);
            assertThat(response.monthlyRevenue()).isEqualTo(5_000_000L);
            assertThat(response.monthlyProfitGrowthRate()).isEqualTo(new BigDecimal("12.50"));
            assertThat(response.businessAgeMonths()).isEqualTo(48);
            assertThat(response.vatFilingStatus()).isEqualTo("FILED");
            assertThat(response.taxOverdue()).isFalse();
            assertThat(response.insurancePaymentStatus()).isEqualTo("PAID");
            assertThat(response.industrySalesRank()).isEqualTo(new BigDecimal("25.00"));
            assertThat(response.industryProfitRank()).isEqualTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("보유 대출이 없으면 existingLoanCount는 0이다")
        void shouldReturnZeroExistingLoanCountWhenNoLoans() {
            // given
            User user = mock(User.class);
            given(user.getUserId()).willReturn(1L);

            LoanApplication app = mock(LoanApplication.class);
            given(app.getBizDataId()).willReturn(100L);
            given(app.getUser()).willReturn(user);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            MyBizData myBizData = mock(MyBizData.class);
            given(myBizData.getAnnualIncome()).willReturn(60_000_000L);
            given(myBizData.getMonthlyRevenue()).willReturn(5_000_000L);
            given(myBizData.getMonthlyProfitGrowthRate()).willReturn(null);
            given(myBizData.getBusinessAgeMonths()).willReturn(null);
            given(myBizData.getVatFilingStatus()).willReturn(null);
            given(myBizData.getTaxOverdue()).willReturn(null);
            given(myBizData.getInsurancePaymentStatus()).willReturn(null);
            given(myBizData.getIndustrySalesRank()).willReturn(null);
            given(myBizData.getIndustryProfitRank()).willReturn(null);
            given(myBizDataRepository.findById(100L)).willReturn(Optional.of(myBizData));

            given(loanApplicationRepository.countByUser_UserIdAndStatus(1L, ApplicationStatus.EXECUTED))
                    .willReturn(0);

            // when
            MyBizDataDetailResponse response = myBizDataDetailService.findMyBizDataDetail(1L);

            // then
            assertThat(response.existingLoanCount()).isZero();
        }
    }
}
