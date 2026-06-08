package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.ApplicationInfoResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.DecisionResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.ProductInfoResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse.RecommendationResponse;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.LoanProductOption;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("LoanApplicationReviewConverter 단위 테스트")
class LoanApplicationReviewConverterTest {

    @Nested
    @DisplayName("toProductInfoResponse")
    class ToProductInfoResponseTest {

        @Test
        @DisplayName("상품 정보와 옵션을 ProductInfoResponse로 변환한다")
        void shouldConvertProductAndOptions() {
            // given
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductName()).willReturn("소상공인 대출");
            given(product.getMinLimit()).willReturn(1_000_000L);
            given(product.getMaxLimit()).willReturn(100_000_000L);
            given(product.getMinRate()).willReturn(new BigDecimal("2.5"));
            given(product.getMaxRate()).willReturn(new BigDecimal("8.0"));
            given(product.getMinTerm()).willReturn(12);
            given(product.getMaxTerm()).willReturn(60);

            LoanProductOption option1 = mock(LoanProductOption.class);
            given(option1.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);
            given(option1.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);

            LoanProductOption option2 = mock(LoanProductOption.class);
            given(option2.getRepaymentMethod()).willReturn(RepaymentMethod.BULLET);
            given(option2.getPurpose()).willReturn(LoanPurpose.FACILITY_CAPITAL);

            // 중복 옵션
            LoanProductOption option3 = mock(LoanProductOption.class);
            given(option3.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);
            given(option3.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);

            // when
            ProductInfoResponse response = LoanApplicationReviewConverter
                    .toProductInfoResponse(product, List.of(option1, option2, option3));

            // then
            assertThat(response.productName()).isEqualTo("소상공인 대출");
            assertThat(response.minAmount()).isEqualTo(1_000_000L);
            assertThat(response.maxAmount()).isEqualTo(100_000_000L);
            assertThat(response.availableRepaymentMethods()).containsExactlyInAnyOrder("EQUAL_PAYMENT", "BULLET");
            assertThat(response.availablePurposes()).containsExactlyInAnyOrder("WORKING_CAPITAL", "FACILITY_CAPITAL");
        }
    }

    @Nested
    @DisplayName("toApplicationInfoResponse")
    class ToApplicationInfoResponseTest {

        @Test
        @DisplayName("LoanApplication을 ApplicationInfoResponse로 변환한다")
        void shouldConvertApplication() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(LoanPurpose.WORKING_CAPITAL);
            given(app.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);

            // when
            ApplicationInfoResponse response = LoanApplicationReviewConverter.toApplicationInfoResponse(app);

            // then
            assertThat(response.requestedAmount()).isEqualTo(50_000_000L);
            assertThat(response.requestedTerm()).isEqualTo(36);
            assertThat(response.purpose()).isEqualTo("WORKING_CAPITAL");
            assertThat(response.repaymentMethod()).isEqualTo("EQUAL_PAYMENT");
        }

        @Test
        @DisplayName("purpose와 repaymentMethod가 null이면 null을 반환한다")
        void shouldReturnNullForNullEnums() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getRequestedAmount()).willReturn(50_000_000L);
            given(app.getRequestedTerm()).willReturn(36);
            given(app.getPurpose()).willReturn(null);
            given(app.getRepaymentMethod()).willReturn(null);

            // when
            ApplicationInfoResponse response = LoanApplicationReviewConverter.toApplicationInfoResponse(app);

            // then
            assertThat(response.purpose()).isNull();
            assertThat(response.repaymentMethod()).isNull();
        }
    }

    @Nested
    @DisplayName("toRecommendationResponse")
    class ToRecommendationResponseTest {

        @Test
        @DisplayName("null 입력 시 null을 반환한다")
        void shouldReturnNullForNullInput() {
            // when
            RecommendationResponse response = LoanApplicationReviewConverter.toRecommendationResponse(null);

            // then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("created_by가 null이 아니면 null을 반환한다 (은행원 심사)")
        void shouldReturnNullForBankerDecision() {
            // given
            LoanDecision decision = mock(LoanDecision.class);
            given(decision.getCreatedBy()).willReturn(50L);

            // when
            RecommendationResponse response = LoanApplicationReviewConverter.toRecommendationResponse(decision);

            // then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("시스템 심사이지만 REJECTED이면 null을 반환한다")
        void shouldReturnNullForSystemRejected() {
            // given
            LoanDecision decision = mock(LoanDecision.class);
            given(decision.getCreatedBy()).willReturn(null);
            given(decision.getStatus()).willReturn(DecisionStatus.SYSTEM_REJECTED);

            // when
            RecommendationResponse response = LoanApplicationReviewConverter.toRecommendationResponse(decision);

            // then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("시스템 승인 건이면 RecommendationResponse를 반환한다")
        void shouldReturnRecommendationForSystemApproved() {
            // given
            LoanDecision decision = mock(LoanDecision.class);
            given(decision.getCreatedBy()).willReturn(null);
            given(decision.getStatus()).willReturn(DecisionStatus.SYSTEM_APPROVED);
            given(decision.getApprovedAmount()).willReturn(45_000_000L);
            given(decision.getApprovedRate()).willReturn(new BigDecimal("4.5"));
            given(decision.getApprovedTerm()).willReturn(36);
            given(decision.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);

            // when
            RecommendationResponse response = LoanApplicationReviewConverter.toRecommendationResponse(decision);

            // then
            assertThat(response).isNotNull();
            assertThat(response.approvedAmount()).isEqualTo(45_000_000L);
            assertThat(response.approvedRate()).isEqualTo(new BigDecimal("4.5"));
            assertThat(response.approvedTerm()).isEqualTo(36);
            assertThat(response.repaymentMethod()).isEqualTo("EQUAL_PAYMENT");
        }

        @Test
        @DisplayName("시스템 승인 건에서 repaymentMethod가 null이면 null을 반환한다")
        void shouldReturnNullRepaymentMethodWhenNull() {
            // given
            LoanDecision decision = mock(LoanDecision.class);
            given(decision.getCreatedBy()).willReturn(null);
            given(decision.getStatus()).willReturn(DecisionStatus.SYSTEM_APPROVED);
            given(decision.getApprovedAmount()).willReturn(45_000_000L);
            given(decision.getApprovedRate()).willReturn(new BigDecimal("4.5"));
            given(decision.getApprovedTerm()).willReturn(36);
            given(decision.getRepaymentMethod()).willReturn(null);

            // when
            RecommendationResponse response = LoanApplicationReviewConverter.toRecommendationResponse(decision);

            // then
            assertThat(response).isNotNull();
            assertThat(response.repaymentMethod()).isNull();
        }
    }

    @Nested
    @DisplayName("toDecisionResponse")
    class ToDecisionResponseTest {

        @Test
        @DisplayName("시스템 심사(created_by == null) + APPROVED이면 status는 SYSTEM_APPROVED이다")
        void shouldReturnSystemApprovedStatus() {
            // given
            LoanDecision decision = mock(LoanDecision.class);
            given(decision.getCreatedBy()).willReturn(null);
            given(decision.getStatus()).willReturn(DecisionStatus.SYSTEM_APPROVED);
            given(decision.getComment()).willReturn("시스템 자동 승인");
            given(decision.getCreatedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            // when
            DecisionResponse response = LoanApplicationReviewConverter.toDecisionResponse(decision, null);

            // then
            assertThat(response.status()).isEqualTo("SYSTEM_APPROVED");
            assertThat(response.reviewerName()).isEqualTo("시스템");
            assertThat(response.reviewerRole()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("시스템 심사(created_by == null) + REJECTED이면 status는 REJECTED이다")
        void shouldReturnRejectedStatusForSystemRejected() {
            // given
            LoanDecision decision = mock(LoanDecision.class);
            given(decision.getCreatedBy()).willReturn(null);
            given(decision.getStatus()).willReturn(DecisionStatus.SYSTEM_REJECTED);
            given(decision.getComment()).willReturn("시스템 자동 거절");
            given(decision.getCreatedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            // when
            DecisionResponse response = LoanApplicationReviewConverter.toDecisionResponse(decision, null);

            // then
            assertThat(response.status()).isEqualTo("SYSTEM_REJECTED");
            assertThat(response.reviewerName()).isEqualTo("시스템");
            assertThat(response.reviewerRole()).isEqualTo("SYSTEM");
        }

        @Test
        @DisplayName("은행원 심사 + User 존재 시 reviewer 정보를 매핑한다")
        void shouldMapBankerInfo() {
            // given
            LoanDecision decision = mock(LoanDecision.class);
            given(decision.getCreatedBy()).willReturn(50L);
            given(decision.getStatus()).willReturn(DecisionStatus.TELLER_APPROVED);
            given(decision.getComment()).willReturn("승인합니다.");
            given(decision.getCreatedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            User banker = mock(User.class);
            given(banker.getName()).willReturn("김은행");
            given(banker.getRole()).willReturn(UserRole.ADMIN_BANK_TELLER);

            // when
            DecisionResponse response = LoanApplicationReviewConverter.toDecisionResponse(decision, banker);

            // then
            assertThat(response.status()).isEqualTo("TELLER_APPROVED");
            assertThat(response.reviewerName()).isEqualTo("김은행");
            assertThat(response.reviewerRole()).isEqualTo("ADMIN_BANK_TELLER");
        }

        @Test
        @DisplayName("은행원 심사 + User 미존재 시 '알 수 없음'으로 표시한다")
        void shouldShowUnknownWhenUserNotFound() {
            // given
            LoanDecision decision = mock(LoanDecision.class);
            given(decision.getCreatedBy()).willReturn(50L);
            given(decision.getStatus()).willReturn(DecisionStatus.TELLER_REJECTED);
            given(decision.getComment()).willReturn("거절합니다.");
            given(decision.getCreatedAt()).willReturn(LocalDateTime.of(2025, 6, 1, 10, 0));

            // when
            DecisionResponse response = LoanApplicationReviewConverter.toDecisionResponse(decision, null);

            // then
            assertThat(response.status()).isEqualTo("TELLER_REJECTED");
            assertThat(response.reviewerName()).isEqualTo("알 수 없음");
            assertThat(response.reviewerRole()).isEqualTo("ADMIN_BANK_TELLER");
        }
    }
}
