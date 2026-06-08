package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationInfoResponse;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.IncomeType;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("LoanApplicationInfoConverter 단위 테스트")
class LoanApplicationInfoConverterTest {

    @Nested
    @DisplayName("toApplicantInfo")
    class ToApplicantInfoTest {

        @Test
        @DisplayName("User를 ApplicantInfo로 변환한다")
        void shouldConvertUserToApplicantInfo() {
            // given
            User user = mock(User.class);
            given(user.getName()).willReturn("홍길동");
            given(user.getResidentNumber()).willReturn("9001011");
            given(user.getPhoneNumber()).willReturn("01012345678");
            given(user.getCreatedAt()).willReturn(LocalDateTime.of(2025, 1, 1, 0, 0));
            given(user.getLoginId()).willReturn("hong123");

            // when
            LoanApplicationInfoResponse.ApplicantInfo result = LoanApplicationInfoConverter.toApplicantInfo(user);

            // then
            assertThat(result.name()).isEqualTo("홍길동");
            assertThat(result.residentNumber()).isEqualTo("9001011");
            assertThat(result.phoneNumber()).isEqualTo("01012345678");
            assertThat(result.loginId()).isEqualTo("hong123");
        }
    }

    @Nested
    @DisplayName("toBusinessInfo")
    class ToBusinessInfoTest {

        @Test
        @DisplayName("BusinessProfile을 BusinessInfo로 변환한다")
        void shouldConvertBusinessProfile() {
            // given
            BusinessProfile bp = mock(BusinessProfile.class);
            given(bp.getBusinessName()).willReturn("길동상회");
            given(bp.getBusinessNumber()).willReturn("1234567890");
            given(bp.getBusinessCategory()).willReturn("음식점업");
            given(bp.getBusinessType()).willReturn("한식");
            given(bp.getBusinessAddress()).willReturn("서울시 강남구");
            given(bp.getOpenDate()).willReturn(LocalDate.of(2020, 3, 15));

            // when
            LoanApplicationInfoResponse.BusinessInfo result = LoanApplicationInfoConverter.toBusinessInfo(bp);

            // then
            assertThat(result.businessName()).isEqualTo("길동상회");
            assertThat(result.businessNumber()).isEqualTo("1234567890");
            assertThat(result.businessCategory()).isEqualTo("음식점업");
            assertThat(result.openDate()).isEqualTo(LocalDate.of(2020, 3, 15));
        }
    }

    @Nested
    @DisplayName("toApplicationInfo")
    class ToApplicationInfoTest {

        @Test
        @DisplayName("LoanApplication을 ApplicationInfo로 변환한다")
        void shouldConvertLoanApplication() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);
            given(app.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);

            // when
            LoanApplicationInfoResponse.ApplicationInfo result = LoanApplicationInfoConverter.toApplicationInfo(app);

            // then
            assertThat(result.requestedAmount()).isEqualTo(50_000_000L);
            assertThat(result.requestedTerm()).isEqualTo(36);
            assertThat(result.purpose()).isEqualTo("WORKING_CAPITAL");
            assertThat(result.repaymentMethod()).isEqualTo("EQUAL_PAYMENT");
        }

        @Test
        @DisplayName("purpose와 repaymentMethod가 null이면 null을 반환한다")
        void shouldHandleNullEnums() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(null);
            given(app.getRepaymentMethod()).willReturn(null);

            // when
            LoanApplicationInfoResponse.ApplicationInfo result = LoanApplicationInfoConverter.toApplicationInfo(app);

            // then
            assertThat(result.purpose()).isNull();
            assertThat(result.repaymentMethod()).isNull();
        }
    }

    @Nested
    @DisplayName("toUserInputInfo")
    class ToUserInputInfoTest {

        @Test
        @DisplayName("모든 UserInput 필드가 non-null이면 name/code를 반환한다")
        void shouldReturnNamesWhenAllFieldsNonNull() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getUserInputAnnualIncome()).willReturn("AMT_30_50M");
            given(app.getUserInputCreditScore()).willReturn("CS_850_OVER");
            given(app.getUserInputIncomeType()).willReturn(com.sofit.common.entity.loan.enums.IncomeType.BUSINESS);
            given(app.getUserInputExistingLoanAmt()).willReturn("LOAN_0_100M");

            // when
            LoanApplicationInfoResponse.UserInputInfo result = LoanApplicationInfoConverter.toUserInputInfo(app);

            // then
            assertThat(result.annualIncome()).isEqualTo("AMT_30_50M");
            assertThat(result.creditScore()).isEqualTo("CS_850_OVER");
            assertThat(result.incomeType()).isEqualTo("02");
            assertThat(result.existingLoanAmount()).isEqualTo("LOAN_0_100M");
        }

        @Test
        @DisplayName("모든 UserInput 필드가 null이면 null을 반환한다")
        void shouldReturnNullWhenAllFieldsNull() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getUserInputAnnualIncome()).willReturn(null);
            given(app.getUserInputCreditScore()).willReturn(null);
            given(app.getUserInputIncomeType()).willReturn(null);
            given(app.getUserInputExistingLoanAmt()).willReturn(null);

            // when
            LoanApplicationInfoResponse.UserInputInfo result = LoanApplicationInfoConverter.toUserInputInfo(app);

            // then
            assertThat(result.annualIncome()).isNull();
            assertThat(result.creditScore()).isNull();
            assertThat(result.incomeType()).isNull();
            assertThat(result.existingLoanAmount()).isNull();
        }
    }

    @Nested
    @DisplayName("toConsentHistories")
    class ToConsentHistoriesTest {

        @Test
        @DisplayName("null 입력 시 빈 리스트를 반환한다")
        void shouldReturnEmptyListForNull() {
            // when
            List<LoanApplicationInfoResponse.ConsentHistoryItem> result =
                    LoanApplicationInfoConverter.toConsentHistories(null);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("빈 리스트 입력 시 빈 리스트를 반환한다")
        void shouldReturnEmptyListForEmptyInput() {
            // when
            List<LoanApplicationInfoResponse.ConsentHistoryItem> result =
                    LoanApplicationInfoConverter.toConsentHistories(Collections.emptyList());

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("동의한 약관은 consentedAt을 포함한다")
        void shouldIncludeConsentedAtWhenConsented() {
            // given
            Term term = mock(Term.class);
            given(term.getTitle()).willReturn("개인정보 수집 동의");
            given(term.getIsRequired()).willReturn(true);

            ConsentHistory consent = mock(ConsentHistory.class);
            given(consent.getTerm()).willReturn(term);
            given(consent.getIsConsented()).willReturn(true);
            given(consent.getConsentedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            // when
            List<LoanApplicationInfoResponse.ConsentHistoryItem> result =
                    LoanApplicationInfoConverter.toConsentHistories(List.of(consent));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("개인정보 수집 동의");
            assertThat(result.get(0).isRequired()).isTrue();
            assertThat(result.get(0).isConsented()).isTrue();
            assertThat(result.get(0).consentedAt()).isNotNull();
        }

        @Test
        @DisplayName("미동의한 약관은 consentedAt이 null이다")
        void shouldReturnNullConsentedAtWhenNotConsented() {
            // given
            Term term = mock(Term.class);
            given(term.getTitle()).willReturn("마케팅 동의");
            given(term.getIsRequired()).willReturn(false);

            ConsentHistory consent = mock(ConsentHistory.class);
            given(consent.getTerm()).willReturn(term);
            given(consent.getIsConsented()).willReturn(false);

            // when
            List<LoanApplicationInfoResponse.ConsentHistoryItem> result =
                    LoanApplicationInfoConverter.toConsentHistories(List.of(consent));

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).isConsented()).isFalse();
            assertThat(result.get(0).consentedAt()).isNull();
        }
    }
}
