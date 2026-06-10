package com.sofit.user.domain.loan.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.sofit.common.entity.loan.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.user.User;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;

class LoanConverterTest {

    private static final Long APPLICATION_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 1L;

    // === toListItem ===

    @Test
    @DisplayName("toListItem - appliedAt이 있으면 LocalDate로 변환된다")
    void toListItem_withAppliedAt_convertsToLocalDate() {
        // given
        LoanApplication application = createSubmittedApplication();
        LocalDateTime appliedAt = LocalDateTime.of(2024, 5, 15, 10, 30);
        ReflectionTestUtils.setField(application, "appliedAt", appliedAt);

        // when
        LoanApplicationListResponse.LoanApplicationItem item = LoanConverter.toListItem(application);

        // then
        assertThat(item.getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(item.getProductName()).isEqualTo("소상공인 성장 대출");
        assertThat(item.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(item.getRequestedAmount()).isEqualTo(30_000_000L);
        assertThat(item.getAppliedAt()).isEqualTo(appliedAt.toLocalDate());
    }

    @Test
    @DisplayName("toListItem - appliedAt이 null이면 appliedAt도 null")
    void toListItem_withNullAppliedAt_returnsNullAppliedAt() {
        // given
        LoanApplication application = createDraftApplication();
        // DRAFT 상태는 appliedAt이 null

        // when
        LoanApplicationListResponse.LoanApplicationItem item = LoanConverter.toListItem(application);

        // then
        assertThat(item.getAppliedAt()).isNull();
    }

    // === toDetailResponse ===

    @Test
    @DisplayName("toDetailResponse - 상세 필드가 정확히 매핑된다")
    void toDetailResponse_mapsFieldsCorrectly() {
        // given
        LoanApplication application = createSubmittedApplication();
        LocalDateTime appliedAt = LocalDateTime.of(2024, 5, 15, 10, 30);
        ReflectionTestUtils.setField(application, "appliedAt", appliedAt);

        // when
        LoanApplicationDetailResponse response = LoanConverter.toDetailResponse(application);

        // then
        assertThat(response.getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.getProductName()).isEqualTo("소상공인 성장 대출");
        assertThat(response.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(response.getRequestedAmount()).isEqualTo(30_000_000L);
        assertThat(response.getRequestedTerm()).isEqualTo(36);
        assertThat(response.getRepaymentMethod()).isEqualTo(RepaymentMethod.EQUAL_PAYMENT);
        assertThat(response.getAppliedAt()).isEqualTo(appliedAt);
    }

    // === toCompletedListItem ===

    @Test
    @DisplayName("toCompletedListItem - appliedAt과 updatedAt이 LocalDate로 변환된다")
    void toCompletedListItem_convertsDatesToLocalDate() {
        // given
        LoanApplication application = createSubmittedApplication();
        LocalDateTime appliedAt = LocalDateTime.of(2024, 5, 15, 10, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 5, 20, 14, 0);
        ReflectionTestUtils.setField(application, "appliedAt", appliedAt);
        ReflectionTestUtils.setField(application, "updatedAt", updatedAt);

        // when
        CompletedLoanListResponse.CompletedLoanItem item = LoanConverter.toCompletedListItem(application);

        // then
        assertThat(item.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(item.productName()).isEqualTo("소상공인 성장 대출");
        assertThat(item.appliedAt()).isEqualTo(appliedAt.toLocalDate());
        assertThat(item.updatedAt()).isEqualTo(updatedAt.toLocalDate());
    }

    @Test
    @DisplayName("toCompletedListItem - appliedAt과 updatedAt이 null이면 null 반환")
    void toCompletedListItem_withNullDates_returnsNullDates() {
        // given
        LoanApplication application = createDraftApplication();
        // appliedAt, updatedAt 모두 null

        // when
        CompletedLoanListResponse.CompletedLoanItem item = LoanConverter.toCompletedListItem(application);

        // then
        assertThat(item.appliedAt()).isNull();
        assertThat(item.updatedAt()).isNull();
    }

    // === toCompletedDetailResponse ===

    @Test
    @DisplayName("toCompletedDetailResponse - 승인 결정 정보가 정확히 매핑된다")
    void toCompletedDetailResponse_mapsApprovalDecisionCorrectly() {
        // given
        LoanApplication application = createSubmittedApplication();
        LoanDecision decision = LoanDecision.createApproval(
                application,
                DecisionStatus.TELLER_APPROVED,
                25_000_000L,
                new BigDecimal("4.50"),
                36,
                RepaymentMethod.EQUAL_PAYMENT,
                "심사 승인",
                1L
        );

        // when
        CompletedLoanDetailResponse response = LoanConverter.toCompletedDetailResponse(application, decision);

        // then
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.productName()).isEqualTo("소상공인 성장 대출");
        assertThat(response.requestedAmount()).isEqualTo(30_000_000L);
        assertThat(response.repaymentMethod()).isEqualTo(RepaymentMethod.EQUAL_PAYMENT);

        CompletedLoanDetailResponse.DecisionInfo decisionInfo = response.decisionInfo();
        assertThat(decisionInfo.decision()).isEqualTo(DecisionStatus.TELLER_APPROVED);
        assertThat(decisionInfo.approvedAmount()).isEqualTo(25_000_000L);
        assertThat(decisionInfo.approvedRate()).isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(decisionInfo.approvedTerm()).isEqualTo(36);
        assertThat(decisionInfo.comment()).isEqualTo("심사 승인");
    }

    @Test
    @DisplayName("toCompletedDetailResponse - 거절 결정 정보가 정확히 매핑된다")
    void toCompletedDetailResponse_mapsRejectionDecisionCorrectly() {
        // given
        LoanApplication application = createSubmittedApplication();
        LoanDecision decision = LoanDecision.createRejection(application, DecisionStatus.SYSTEM_REJECTED, "신용점수 미달", null);

        // when
        CompletedLoanDetailResponse response = LoanConverter.toCompletedDetailResponse(application, decision);

        // then
        CompletedLoanDetailResponse.DecisionInfo decisionInfo = response.decisionInfo();
        assertThat(decisionInfo.decision()).isEqualTo(DecisionStatus.SYSTEM_REJECTED);
        assertThat(decisionInfo.approvedAmount()).isNull();
        assertThat(decisionInfo.approvedRate()).isNull();
        assertThat(decisionInfo.comment()).isEqualTo("신용점수 미달");
    }

    // --- 헬퍼 메서드 ---

    private LoanApplication createDraftApplication() {
        User user = User.createUser("testuser", "hashedpw", "홍길동", "01012345678", "9901011");
        ReflectionTestUtils.setField(user, "userId", USER_ID);

        LoanProduct product = createProduct();

        LoanApplication application = LoanApplication.createDraft(
                user, product,
                "AMT_30_50M",
                "CS_0_850",
                IncomeType.SALARY,
                "LOAN_0_100M"
        );
        ReflectionTestUtils.setField(application, "applicationId", APPLICATION_ID);
        return application;
    }

    private LoanApplication createSubmittedApplication() {
        LoanApplication application = createDraftApplication();
        application.submit(30_000_000L, 36, RepaymentMethod.EQUAL_PAYMENT, LoanPurpose.WORKING_CAPITAL);
        return application;
    }

    private LoanProduct createProduct() {
        LoanProduct product;
        try {
            var constructor = LoanProduct.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            product = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("LoanProduct 인스턴스 생성 실패", e);
        }
        ReflectionTestUtils.setField(product, "productId", PRODUCT_ID);
        ReflectionTestUtils.setField(product, "productName", "소상공인 성장 대출");
        ReflectionTestUtils.setField(product, "status", ProductStatus.ACTIVE);
        return product;
    }
}
