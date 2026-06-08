package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.LoanProductOption;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.loan.LoanProductOptionRepository;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoanApplicationReviewServiceImpl 단위 테스트")
class LoanApplicationReviewServiceImplTest {

    @InjectMocks
    private LoanApplicationReviewServiceImpl loanApplicationReviewService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanProductOptionRepository loanProductOptionRepository;

    @Mock
    private LoanDecisionRepository loanDecisionRepository;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("findLoanApplicationReview")
    class FindLoanApplicationReviewTest {

        @Test
        @DisplayName("LoanApplication이 존재하지 않으면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenApplicationNotExists() {
            // given
            given(loanApplicationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationReviewService.findLoanApplicationReview(999L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("심사 이력이 없으면 빈 decisions 리스트와 null recommendation을 반환한다")
        void shouldReturnEmptyDecisionsWhenNoDecisions() {
            // given
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductId()).willReturn(1L);
            given(product.getProductName()).willReturn("소상공인 대출");
            given(product.getMinLimit()).willReturn(1_000_000L);
            given(product.getMaxLimit()).willReturn(100_000_000L);
            given(product.getMinRate()).willReturn(new BigDecimal("2.5"));
            given(product.getMaxRate()).willReturn(new BigDecimal("8.0"));
            given(product.getMinTerm()).willReturn(12);
            given(product.getMaxTerm()).willReturn(60);

            LoanApplication app = mock(LoanApplication.class);
            given(app.getProduct()).willReturn(product);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);
            given(app.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(loanProductOptionRepository.findByProduct_ProductId(1L)).willReturn(Collections.emptyList());
            given(loanDecisionRepository.findAllByApplication_ApplicationIdOrderByCreatedAtAsc(10L))
                    .willReturn(Collections.emptyList());

            // when
            LoanApplicationReviewResponse response = loanApplicationReviewService.findLoanApplicationReview(10L);

            // then
            assertThat(response.productInfo().productName()).isEqualTo("소상공인 대출");
            assertThat(response.applicationInfo().requestedAmount()).isEqualTo(50_000_000L);
            assertThat(response.recommendation()).isNull();
            assertThat(response.decisions()).isEmpty();
        }

        @Test
        @DisplayName("시스템 승인 건이 있으면 recommendation을 반환한다")
        void shouldReturnRecommendationWhenSystemApproved() {
            // given
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductId()).willReturn(1L);
            given(product.getProductName()).willReturn("소상공인 대출");
            given(product.getMinLimit()).willReturn(1_000_000L);
            given(product.getMaxLimit()).willReturn(100_000_000L);
            given(product.getMinRate()).willReturn(new BigDecimal("2.5"));
            given(product.getMaxRate()).willReturn(new BigDecimal("8.0"));
            given(product.getMinTerm()).willReturn(12);
            given(product.getMaxTerm()).willReturn(60);

            LoanApplication app = mock(LoanApplication.class);
            given(app.getProduct()).willReturn(product);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);
            given(app.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);

            // 시스템 심사 (created_by == null, SYSTEM_APPROVED)
            LoanDecision systemDecision = mock(LoanDecision.class);
            given(systemDecision.getCreatedBy()).willReturn(null);
            given(systemDecision.getStatus()).willReturn(DecisionStatus.SYSTEM_APPROVED);
            given(systemDecision.getApprovedAmount()).willReturn(45_000_000L);
            given(systemDecision.getApprovedRate()).willReturn(new BigDecimal("4.5"));
            given(systemDecision.getApprovedTerm()).willReturn(36);
            given(systemDecision.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);
            given(systemDecision.getComment()).willReturn("시스템 자동 승인");
            given(systemDecision.getCreatedAt()).willReturn(null);

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(loanProductOptionRepository.findByProduct_ProductId(1L)).willReturn(Collections.emptyList());
            given(loanDecisionRepository.findAllByApplication_ApplicationIdOrderByCreatedAtAsc(10L))
                    .willReturn(List.of(systemDecision));

            // when
            LoanApplicationReviewResponse response = loanApplicationReviewService.findLoanApplicationReview(10L);

            // then
            assertThat(response.recommendation()).isNotNull();
            assertThat(response.recommendation().approvedAmount()).isEqualTo(45_000_000L);
            assertThat(response.recommendation().approvedRate()).isEqualTo(new BigDecimal("4.5"));
            assertThat(response.decisions()).hasSize(1);
            assertThat(response.decisions().get(0).status()).isEqualTo("SYSTEM_APPROVED");
            assertThat(response.decisions().get(0).reviewerName()).isEqualTo("시스템");
        }

        @Test
        @DisplayName("시스템 REJECTED 건만 있으면 recommendation은 null이다")
        void shouldReturnNullRecommendationWhenSystemRejected() {
            // given
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductId()).willReturn(1L);
            given(product.getProductName()).willReturn("소상공인 대출");
            given(product.getMinLimit()).willReturn(1_000_000L);
            given(product.getMaxLimit()).willReturn(100_000_000L);
            given(product.getMinRate()).willReturn(new BigDecimal("2.5"));
            given(product.getMaxRate()).willReturn(new BigDecimal("8.0"));
            given(product.getMinTerm()).willReturn(12);
            given(product.getMaxTerm()).willReturn(60);

            LoanApplication app = mock(LoanApplication.class);
            given(app.getProduct()).willReturn(product);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);
            given(app.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);

            // 시스템 심사 (created_by == null, SYSTEM_REJECTED) — recommendation 대상 아님
            LoanDecision systemRejected = mock(LoanDecision.class);
            given(systemRejected.getCreatedBy()).willReturn(null);
            given(systemRejected.getStatus()).willReturn(DecisionStatus.SYSTEM_REJECTED);
            given(systemRejected.getComment()).willReturn("시스템 자동 거절");
            given(systemRejected.getCreatedAt()).willReturn(null);

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(loanProductOptionRepository.findByProduct_ProductId(1L)).willReturn(Collections.emptyList());
            given(loanDecisionRepository.findAllByApplication_ApplicationIdOrderByCreatedAtAsc(10L))
                    .willReturn(List.of(systemRejected));

            // when
            LoanApplicationReviewResponse response = loanApplicationReviewService.findLoanApplicationReview(10L);

            // then
            assertThat(response.recommendation()).isNull();
            assertThat(response.decisions()).hasSize(1);
            assertThat(response.decisions().get(0).status()).isEqualTo("SYSTEM_REJECTED");
            assertThat(response.decisions().get(0).reviewerName()).isEqualTo("시스템");
        }

        @Test
        @DisplayName("은행원 심사 이력이 있으면 reviewer 정보를 매핑한다")
        void shouldMapReviewerInfoForBankerDecision() {
            // given
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductId()).willReturn(1L);
            given(product.getProductName()).willReturn("소상공인 대출");
            given(product.getMinLimit()).willReturn(1_000_000L);
            given(product.getMaxLimit()).willReturn(100_000_000L);
            given(product.getMinRate()).willReturn(new BigDecimal("2.5"));
            given(product.getMaxRate()).willReturn(new BigDecimal("8.0"));
            given(product.getMinTerm()).willReturn(12);
            given(product.getMaxTerm()).willReturn(60);

            LoanApplication app = mock(LoanApplication.class);
            given(app.getProduct()).willReturn(product);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);
            given(app.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);

            // 은행원 심사 (created_by != null)
            LoanDecision bankerDecision = mock(LoanDecision.class);
            given(bankerDecision.getCreatedBy()).willReturn(50L);
            given(bankerDecision.getStatus()).willReturn(DecisionStatus.TELLER_APPROVED);
            given(bankerDecision.getComment()).willReturn("승인합니다.");
            given(bankerDecision.getCreatedAt()).willReturn(null);

            User banker = mock(User.class);
            given(banker.getUserId()).willReturn(50L);
            given(banker.getName()).willReturn("김은행");
            given(banker.getRole()).willReturn(UserRole.ADMIN_BANK_TELLER);

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(loanProductOptionRepository.findByProduct_ProductId(1L)).willReturn(Collections.emptyList());
            given(loanDecisionRepository.findAllByApplication_ApplicationIdOrderByCreatedAtAsc(10L))
                    .willReturn(List.of(bankerDecision));
            given(userRepository.findAllById(List.of(50L))).willReturn(List.of(banker));

            // when
            LoanApplicationReviewResponse response = loanApplicationReviewService.findLoanApplicationReview(10L);

            // then
            assertThat(response.decisions()).hasSize(1);
            assertThat(response.decisions().get(0).reviewerName()).isEqualTo("김은행");
            assertThat(response.decisions().get(0).reviewerRole()).isEqualTo("ADMIN_BANK_TELLER");
            assertThat(response.decisions().get(0).status()).isEqualTo("TELLER_APPROVED");
        }

        @Test
        @DisplayName("LoanProductOption이 있으면 availableRepaymentMethods와 availablePurposes를 매핑한다")
        void shouldMapProductOptions() {
            // given
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductId()).willReturn(1L);
            given(product.getProductName()).willReturn("소상공인 대출");
            given(product.getMinLimit()).willReturn(1_000_000L);
            given(product.getMaxLimit()).willReturn(100_000_000L);
            given(product.getMinRate()).willReturn(new BigDecimal("2.5"));
            given(product.getMaxRate()).willReturn(new BigDecimal("8.0"));
            given(product.getMinTerm()).willReturn(12);
            given(product.getMaxTerm()).willReturn(60);

            LoanApplication app = mock(LoanApplication.class);
            given(app.getProduct()).willReturn(product);
            given(app.getApplicationId()).willReturn(10L);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);
            given(app.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);

            LoanProductOption option1 = mock(LoanProductOption.class);
            given(option1.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);
            given(option1.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);

            LoanProductOption option2 = mock(LoanProductOption.class);
            given(option2.getRepaymentMethod()).willReturn(RepaymentMethod.BULLET);
            given(option2.getPurpose()).willReturn(LoanPurpose.FACILITY_CAPITAL);

            given(loanApplicationRepository.findById(10L)).willReturn(Optional.of(app));
            given(loanProductOptionRepository.findByProduct_ProductId(1L)).willReturn(List.of(option1, option2));
            given(loanDecisionRepository.findAllByApplication_ApplicationIdOrderByCreatedAtAsc(10L))
                    .willReturn(Collections.emptyList());

            // when
            LoanApplicationReviewResponse response = loanApplicationReviewService.findLoanApplicationReview(10L);

            // then
            assertThat(response.productInfo().availableRepaymentMethods())
                    .containsExactlyInAnyOrder("EQUAL_PAYMENT", "BULLET");
            assertThat(response.productInfo().availablePurposes())
                    .containsExactlyInAnyOrder("WORKING_CAPITAL", "FACILITY_CAPITAL");
        }
    }
}
