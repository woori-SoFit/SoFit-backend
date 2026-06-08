package com.sofit.user.domain.loan.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.IncomeType;
import com.sofit.common.entity.loan.enums.LastCompletedStep;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.ProductStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.user.User;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;

class LoanApplicationConverterTest {

    private static final Long APPLICATION_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 1L;

    // === toCreateResponse ===

    @Test
    @DisplayName("toCreateResponse - applicationId가 정확히 매핑된다")
    void toCreateResponse_mapsApplicationIdCorrectly() {
        // given
        LoanApplication application = createDraftApplication(null);

        // when
        LoanApplicationCreateResponse response = LoanApplicationConverter.toCreateResponse(application);

        // then
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
    }

    // === toDraftCheckResponse ===

    @Test
    @DisplayName("toDraftCheckResponse - lastCompletedStep이 null이면 resumeStep은 CONSENT")
    void toDraftCheckResponse_whenStepIsNull_resumeStepIsConsent() {
        // given
        LoanApplication application = createDraftApplication(null);

        // when
        DraftCheckResponse response = LoanApplicationConverter.toDraftCheckResponse(application);

        // then
        assertThat(response.hasDraft()).isTrue();
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.lastCompletedStep()).isNull();
        assertThat(response.resumeStep()).isEqualTo("CONSENT");
    }

    @Test
    @DisplayName("toDraftCheckResponse - CONSENT_DONE이면 resumeStep은 BIZ_INFO")
    void toDraftCheckResponse_whenConsentDone_resumeStepIsBizInfo() {
        // given
        LoanApplication application = createDraftApplication(LastCompletedStep.CONSENT_DONE);

        // when
        DraftCheckResponse response = LoanApplicationConverter.toDraftCheckResponse(application);

        // then
        assertThat(response.hasDraft()).isTrue();
        assertThat(response.lastCompletedStep()).isEqualTo("CONSENT_DONE");
        assertThat(response.resumeStep()).isEqualTo("BIZ_INFO");
    }

    @Test
    @DisplayName("toDraftCheckResponse - BIZ_INFO_DONE이면 resumeStep은 COLLECT_DATA")
    void toDraftCheckResponse_whenBizInfoDone_resumeStepIsCollectData() {
        // given
        LoanApplication application = createDraftApplication(LastCompletedStep.BIZ_INFO_DONE);

        // when
        DraftCheckResponse response = LoanApplicationConverter.toDraftCheckResponse(application);

        // then
        assertThat(response.resumeStep()).isEqualTo("COLLECT_DATA");
    }

    @Test
    @DisplayName("toDraftCheckResponse - DATA_COLLECTED이면 resumeStep은 MYBIZ")
    void toDraftCheckResponse_whenDataCollected_resumeStepIsMybiz() {
        // given
        LoanApplication application = createDraftApplication(LastCompletedStep.DATA_COLLECTED);

        // when
        DraftCheckResponse response = LoanApplicationConverter.toDraftCheckResponse(application);

        // then
        assertThat(response.resumeStep()).isEqualTo("MYBIZ");
    }

    @Test
    @DisplayName("toDraftCheckResponse - MYBIZ_CONNECTED이면 resumeStep은 LOAN_CONDITION")
    void toDraftCheckResponse_whenMybizConnected_resumeStepIsLoanCondition() {
        // given
        LoanApplication application = createDraftApplication(LastCompletedStep.MYBIZ_CONNECTED);

        // when
        DraftCheckResponse response = LoanApplicationConverter.toDraftCheckResponse(application);

        // then
        assertThat(response.resumeStep()).isEqualTo("LOAN_CONDITION");
    }

    // === toResumeResponse ===

    @Test
    @DisplayName("toResumeResponse - 저장된 입력값이 정확히 매핑된다")
    void toResumeResponse_mapsSavedDataCorrectly() {
        // given
        LoanApplication application = createDraftApplication(null);

        // when
        LoanApplicationResumeResponse response = LoanApplicationConverter.toResumeResponse(application);

        // then
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.resumeStep()).isEqualTo("CONSENT");
        assertThat(response.savedData().annualIncome()).isEqualTo("AMT_30_50M");
        assertThat(response.savedData().creditScore()).isEqualTo("CS_0_850");
        assertThat(response.savedData().incomeType()).isEqualTo(IncomeType.SALARY.getCode());
        assertThat(response.savedData().existingLoanAmt()).isEqualTo("LOAN_0_100M");
    }

    @Test
    @DisplayName("toResumeResponse - CONSENT_DONE 이상이면 consentsAgreed=true")
    void toResumeResponse_whenConsentDone_consentsAgreedIsTrue() {
        // given
        LoanApplication application = createDraftApplication(LastCompletedStep.CONSENT_DONE);

        // when
        LoanApplicationResumeResponse response = LoanApplicationConverter.toResumeResponse(application);

        // then
        assertThat(response.savedData().consentsAgreed()).isTrue();
    }

    @Test
    @DisplayName("toResumeResponse - lastCompletedStep이 null이면 consentsAgreed=false")
    void toResumeResponse_whenStepIsNull_consentsAgreedIsFalse() {
        // given
        LoanApplication application = createDraftApplication(null);

        // when
        LoanApplicationResumeResponse response = LoanApplicationConverter.toResumeResponse(application);

        // then
        assertThat(response.savedData().consentsAgreed()).isFalse();
    }

    // === toSubmitResponse ===

    @Test
    @DisplayName("toSubmitResponse - 제출 후 응답 필드가 정확히 매핑된다")
    void toSubmitResponse_mapsFieldsCorrectly() {
        // given
        LoanApplication application = createSubmittedApplication();

        // when
        LoanApplicationSubmitResponse response = LoanApplicationConverter.toSubmitResponse(application);

        // then
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.productName()).isEqualTo("소상공인 성장 대출");
        assertThat(response.requestedAmount()).isEqualTo(30_000_000L);
        assertThat(response.repaymentMethod()).isEqualTo(RepaymentMethod.EQUAL_PAYMENT.name());
        assertThat(response.purpose()).isEqualTo(LoanPurpose.WORKING_CAPITAL.name());
        assertThat(response.requestedTerm()).isEqualTo(36);
        assertThat(response.appliedAt()).isNotNull();
    }

    // --- 헬퍼 메서드 ---

    private LoanApplication createDraftApplication(LastCompletedStep lastCompletedStep) {
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
        ReflectionTestUtils.setField(application, "lastCompletedStep", lastCompletedStep);
        return application;
    }

    private LoanApplication createSubmittedApplication() {
        LoanApplication application = createDraftApplication(LastCompletedStep.MYBIZ_CONNECTED);
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
