package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.entity.mybiz.enums.InsurancePaymentStatus;
import com.sofit.common.entity.mybiz.enums.VatFilingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("MyBizDataDetailConverter 단위 테스트")
class MyBizDataDetailConverterTest {

    @Test
    @DisplayName("정상 데이터로 MyBizDataDetailResponse를 생성한다")
    void shouldConvertToResponse() {
        // given
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

        // when
        MyBizDataDetailResponse response = MyBizDataDetailConverter.toMyBizDataDetailResponse(myBizData, 2);

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
    @DisplayName("enum 필드가 null이면 null 문자열을 반환한다")
    void shouldReturnNullForNullEnums() {
        // given
        MyBizData myBizData = mock(MyBizData.class);
        given(myBizData.getAnnualIncome()).willReturn(null);
        given(myBizData.getMonthlyRevenue()).willReturn(null);
        given(myBizData.getMonthlyProfitGrowthRate()).willReturn(null);
        given(myBizData.getBusinessAgeMonths()).willReturn(null);
        given(myBizData.getVatFilingStatus()).willReturn(null);
        given(myBizData.getTaxOverdue()).willReturn(null);
        given(myBizData.getInsurancePaymentStatus()).willReturn(null);
        given(myBizData.getIndustrySalesRank()).willReturn(null);
        given(myBizData.getIndustryProfitRank()).willReturn(null);

        // when
        MyBizDataDetailResponse response = MyBizDataDetailConverter.toMyBizDataDetailResponse(myBizData, 0);

        // then
        assertThat(response.vatFilingStatus()).isNull();
        assertThat(response.insurancePaymentStatus()).isNull();
        assertThat(response.existingLoanCount()).isZero();
    }
}
