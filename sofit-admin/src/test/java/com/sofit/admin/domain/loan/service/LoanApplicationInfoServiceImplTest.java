package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationInfoResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.term.ConsentHistoryRepository;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoanApplicationInfoServiceImpl 단위 테스트")
class LoanApplicationInfoServiceImplTest {

    @InjectMocks
    private LoanApplicationInfoServiceImpl loanApplicationInfoService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    @Mock
    private ConsentHistoryRepository consentHistoryRepository;

    @Nested
    @DisplayName("findLoanApplicationInfo")
    class FindLoanApplicationInfoTest {

        @Test
        @DisplayName("LoanApplication이 존재하지 않으면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenApplicationNotExists() {
            // given
            given(loanApplicationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationInfoService.findLoanApplicationInfo(999L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("User가 존재하지 않으면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenUserNotExists() {
            // given
            User user = mock(User.class);
            given(user.getUserId()).willReturn(1L);

            LoanApplication app = mock(LoanApplication.class);
            given(app.getUser()).willReturn(user);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));
            given(userRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationInfoService.findLoanApplicationInfo(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("BusinessProfile이 존재하지 않으면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenBusinessProfileNotExists() {
            // given
            User user = mock(User.class);
            given(user.getUserId()).willReturn(1L);

            LoanApplication app = mock(LoanApplication.class);
            given(app.getUser()).willReturn(user);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(businessProfileRepository.findByUser_UserId(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationInfoService.findLoanApplicationInfo(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("정상 조회 시 모든 정보를 포함한 응답을 반환한다")
        void shouldReturnFullInfoResponse() {
            // given
            User user = mock(User.class);
            given(user.getUserId()).willReturn(1L);
            given(user.getName()).willReturn("홍길동");
            given(user.getResidentNumber()).willReturn("9001011");
            given(user.getPhoneNumber()).willReturn("01012345678");
            given(user.getCreatedAt()).willReturn(LocalDateTime.of(2025, 1, 1, 0, 0));
            given(user.getLoginId()).willReturn("hong123");

            LoanApplication app = mock(LoanApplication.class);
            given(app.getUser()).willReturn(user);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);
            given(app.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);
            given(app.getUserInputAnnualIncome()).willReturn(null);
            given(app.getUserInputCreditScore()).willReturn(null);
            given(app.getUserInputIncomeType()).willReturn(null);
            given(app.getUserInputExistingLoanAmt()).willReturn(null);

            BusinessProfile bp = mock(BusinessProfile.class);
            given(bp.getBusinessName()).willReturn("길동상회");
            given(bp.getBusinessNumber()).willReturn("1234567890");
            given(bp.getBusinessCategory()).willReturn("음식점업");
            given(bp.getBusinessType()).willReturn("한식");
            given(bp.getBusinessAddress()).willReturn("서울시 강남구");
            given(bp.getOpenDate()).willReturn(LocalDate.of(2020, 3, 15));

            Term term = mock(Term.class);
            given(term.getTitle()).willReturn("개인정보 수집 동의");
            given(term.getIsRequired()).willReturn(true);

            ConsentHistory consent = mock(ConsentHistory.class);
            given(consent.getTerm()).willReturn(term);
            given(consent.getIsConsented()).willReturn(true);
            given(consent.getConsentedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(businessProfileRepository.findByUser_UserId(1L)).willReturn(Optional.of(bp));
            given(consentHistoryRepository.findByUser_UserIdAndApplication_ApplicationIdOrderByConsentIdAsc(1L, 10L))
                    .willReturn(List.of(consent));

            // when
            LoanApplicationInfoResponse response = loanApplicationInfoService.findLoanApplicationInfo(10L);

            // then
            assertThat(response.applicantInfo().name()).isEqualTo("홍길동");
            assertThat(response.applicantInfo().phoneNumber()).isEqualTo("01012345678");
            assertThat(response.businessInfo().businessName()).isEqualTo("길동상회");
            assertThat(response.businessInfo().businessNumber()).isEqualTo("1234567890");
            assertThat(response.applicationInfo().requestedAmount()).isEqualTo(50_000_000L);
            assertThat(response.applicationInfo().purpose()).isEqualTo("WORKING_CAPITAL");
            assertThat(response.consentHistories()).hasSize(1);
            assertThat(response.consentHistories().get(0).title()).isEqualTo("개인정보 수집 동의");
            assertThat(response.consentHistories().get(0).isConsented()).isTrue();
        }

        @Test
        @DisplayName("약관 동의 이력이 없으면 빈 리스트를 반환한다")
        void shouldReturnEmptyConsentHistoriesWhenNone() {
            // given
            User user = mock(User.class);
            given(user.getUserId()).willReturn(1L);
            given(user.getName()).willReturn("홍길동");
            given(user.getResidentNumber()).willReturn("9001011");
            given(user.getPhoneNumber()).willReturn("01012345678");
            given(user.getCreatedAt()).willReturn(LocalDateTime.of(2025, 1, 1, 0, 0));
            given(user.getLoginId()).willReturn("hong123");

            LoanApplication app = mock(LoanApplication.class);
            given(app.getUser()).willReturn(user);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(null);
            given(app.getRepaymentMethod()).willReturn(null);
            given(app.getUserInputAnnualIncome()).willReturn(null);
            given(app.getUserInputCreditScore()).willReturn(null);
            given(app.getUserInputIncomeType()).willReturn(null);
            given(app.getUserInputExistingLoanAmt()).willReturn(null);

            BusinessProfile bp = mock(BusinessProfile.class);
            given(bp.getBusinessName()).willReturn("길동상회");
            given(bp.getBusinessNumber()).willReturn("1234567890");
            given(bp.getBusinessCategory()).willReturn("음식점업");
            given(bp.getBusinessType()).willReturn("한식");
            given(bp.getBusinessAddress()).willReturn("서울시 강남구");
            given(bp.getOpenDate()).willReturn(LocalDate.of(2020, 3, 15));

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(userRepository.findById(1L)).willReturn(Optional.of(user));
            given(businessProfileRepository.findByUser_UserId(1L)).willReturn(Optional.of(bp));
            given(consentHistoryRepository.findByUser_UserIdAndApplication_ApplicationIdOrderByConsentIdAsc(1L, 10L))
                    .willReturn(Collections.emptyList());

            // when
            LoanApplicationInfoResponse response = loanApplicationInfoService.findLoanApplicationInfo(10L);

            // then
            assertThat(response.consentHistories()).isEmpty();
        }
    }
}
