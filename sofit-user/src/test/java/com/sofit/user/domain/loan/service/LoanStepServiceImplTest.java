package com.sofit.user.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.mybiz.MyBizDataRepository;
import com.sofit.common.entity.mybiz.MyBizData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.IncomeType;
import com.sofit.common.entity.loan.enums.LastCompletedStep;
import com.sofit.common.entity.loan.enums.ProductStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse.ConsentItemResponse;
import com.sofit.user.domain.terms.service.TermService;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import com.sofit.user.domain.user.service.BusinessService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class LoanStepServiceImplTest {

    @InjectMocks
    private LoanStepServiceImpl loanStepService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private TermService termService;

    @Mock
    private BusinessService businessService;

    @Mock
    private MyBizDataRepository myBizDataRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 999L;
    private static final Long APPLICATION_ID = 100L;

    // === processConsent (Step 2) ===

    @Test
    @DisplayName("processConsent - 정상적으로 약관 동의를 처리한다")
    void processConsent_success() {
        // given
        LoanApplication application = createApplication(null); // lastCompletedStep = null
        ConsentCreateRequest request = new ConsentCreateRequest();
        ConsentCreateResponse expectedResponse = new ConsentCreateResponse(
                TermType.LOAN_APPLICATION, APPLICATION_ID, USER_ID, List.of(
                        new ConsentItemResponse(1L, true, LocalDateTime.now())
                ));

        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));
        given(termService.createConsents(USER_ID, request)).willReturn(expectedResponse);

        // when
        ConsentCreateResponse response = loanStepService.processConsent(USER_ID, APPLICATION_ID, request);

        // then
        assertThat(response).isEqualTo(expectedResponse);
        assertThat(application.getLastCompletedStep()).isEqualTo(LastCompletedStep.CONSENT_DONE);
    }

    @Test
    @DisplayName("processConsent - 신청 미존재 시 APPLICATION_NOT_FOUND 예외")
    void processConsent_throwsException_whenNotFound() {
        // given
        ConsentCreateRequest request = new ConsentCreateRequest();
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanStepService.processConsent(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("processConsent - 본인 소유가 아니면 APPLICATION_NOT_OWNED 예외")
    void processConsent_throwsException_whenNotOwned() {
        // given
        LoanApplication application = createApplication(null);
        ConsentCreateRequest request = new ConsentCreateRequest();
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanStepService.processConsent(OTHER_USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.APPLICATION_NOT_OWNED);
                });
    }

    @Test
    @DisplayName("processConsent - DRAFT가 아닌 상태면 APPLICATION_NOT_DRAFT 예외")
    void processConsent_throwsException_whenNotDraft() {
        // given
        LoanApplication application = createApplication(null);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.SUBMITTED);
        ConsentCreateRequest request = new ConsentCreateRequest();
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanStepService.processConsent(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.APPLICATION_NOT_DRAFT);
                });
    }

    @Test
    @DisplayName("processConsent - 단계 순서 위반 시 STEP_ORDER_VIOLATION 예외")
    void processConsent_throwsException_whenStepOrderViolation() {
        // given: lastCompletedStep이 CONSENT_DONE인데 다시 consent 호출 (requiredStep=null이어야 함)
        LoanApplication application = createApplication(LastCompletedStep.CONSENT_DONE);
        ConsentCreateRequest request = new ConsentCreateRequest();
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanStepService.processConsent(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.STEP_ORDER_VIOLATION);
                });
    }

    // === processBizInfo (Step 3) ===

    @Test
    @DisplayName("processBizInfo - 정상적으로 사업자 정보를 반환한다")
    void processBizInfo_success() {
        // given
        LoanApplication application = createApplication(LastCompletedStep.CONSENT_DONE);
        BusinessProfileResponse expectedResponse = new BusinessProfileResponse(
                "1234567890", "원 금융컨설팅", "홍길동", "9901011",
                LocalDate.of(2019, 5, 1), "음식점업", "일반음식점", "서울시 강남구", true);

        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));
        given(businessService.findBusinessProfile(USER_ID)).willReturn(expectedResponse);

        // when
        BusinessProfileResponse response = loanStepService.processBizInfo(USER_ID, APPLICATION_ID);

        // then
        assertThat(response).isEqualTo(expectedResponse);
        assertThat(application.getLastCompletedStep()).isEqualTo(LastCompletedStep.BIZ_INFO_DONE);
    }

    @Test
    @DisplayName("processBizInfo - 단계 순서 위반 시 STEP_ORDER_VIOLATION 예외")
    void processBizInfo_throwsException_whenStepOrderViolation() {
        // given: lastCompletedStep이 null인데 bizInfo 호출 (requiredStep=CONSENT_DONE이어야 함)
        LoanApplication application = createApplication(null);
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanStepService.processBizInfo(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.STEP_ORDER_VIOLATION);
                });
    }

    // === processMydata (Step 4) ===

    @Test
    @DisplayName("processMydata - 정상적으로 마이데이터 약관 동의를 처리한다")
    void processMydata_success() {
        // given
        LoanApplication application = createApplication(LastCompletedStep.BIZ_INFO_DONE);
        ConsentCreateRequest request = new ConsentCreateRequest();
        ConsentCreateResponse expectedResponse = new ConsentCreateResponse(
                TermType.MYDATA, APPLICATION_ID, USER_ID, List.of(
                        new ConsentItemResponse(2L, true, LocalDateTime.now())
                ));

        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));
        given(termService.createConsents(USER_ID, request)).willReturn(expectedResponse);

        // when
        ConsentCreateResponse response = loanStepService.processMydata(USER_ID, APPLICATION_ID, request);

        // then
        assertThat(response).isEqualTo(expectedResponse);
        assertThat(application.getLastCompletedStep()).isEqualTo(LastCompletedStep.DATA_COLLECTED);
    }

    @Test
    @DisplayName("processMydata - 단계 순서 위반 시 STEP_ORDER_VIOLATION 예외")
    void processMydata_throwsException_whenStepOrderViolation() {
        // given: lastCompletedStep이 CONSENT_DONE인데 mydata 호출 (requiredStep=BIZ_INFO_DONE이어야 함)
        LoanApplication application = createApplication(LastCompletedStep.CONSENT_DONE);
        ConsentCreateRequest request = new ConsentCreateRequest();
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanStepService.processMydata(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.STEP_ORDER_VIOLATION);
                });
    }

    // === processMybizData (Step 5) ===

    @Test
    @DisplayName("processMybizData - 정상적으로 마이비즈데이터 연동을 처리한다")
    void processMybizData_success() {
        // given
        LoanApplication application = createApplication(LastCompletedStep.DATA_COLLECTED);
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        BusinessProfile profile = createBusinessProfile("1234567890");
        given(businessProfileRepository.findByUser_UserId(USER_ID)).willReturn(Optional.of(profile));

        MyBizData myBizData = createMyBizData(100L);
        given(myBizDataRepository.findFirstByBusinessNumberOrderByReferenceMonthDesc("1234567890"))
                .willReturn(Optional.of(myBizData));

        // when
        loanStepService.processMybizData(USER_ID, APPLICATION_ID);

        // then
        verify(businessService).connectMybiz(USER_ID);
        assertThat(application.getBizDataId()).isEqualTo(100L);
        assertThat(application.getLastCompletedStep()).isEqualTo(LastCompletedStep.MYBIZ_CONNECTED);
    }

    @Test
    @DisplayName("processMybizData - 단계 순서 위반 시 STEP_ORDER_VIOLATION 예외")
    void processMybizData_throwsException_whenStepOrderViolation() {
        // given: lastCompletedStep이 BIZ_INFO_DONE인데 mybizData 호출 (requiredStep=DATA_COLLECTED이어야 함)
        LoanApplication application = createApplication(LastCompletedStep.BIZ_INFO_DONE);
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanStepService.processMybizData(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.STEP_ORDER_VIOLATION);
                });
    }

    // --- 헬퍼 메서드 ---

    private LoanApplication createApplication(LastCompletedStep lastCompletedStep) {
        User user = User.createUser("testuser", "hashedpw", "홍길동", "01012345678", "9901011");
        ReflectionTestUtils.setField(user, "userId", USER_ID);

        LoanProduct product;
        try {
            var constructor = LoanProduct.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            product = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("LoanProduct 인스턴스 생성 실패", e);
        }
        ReflectionTestUtils.setField(product, "productId", 1L);
        ReflectionTestUtils.setField(product, "status", ProductStatus.ACTIVE);

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

    private MyBizData createMyBizData(Long bizDataId) {
        MyBizData myBizData;
        try {
            var constructor = MyBizData.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            myBizData = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("MyBizData 인스턴스 생성 실패", e);
        }
        ReflectionTestUtils.setField(myBizData, "bizDataId", bizDataId);
        return myBizData;
    }

    private BusinessProfile createBusinessProfile(String businessNumber) {
        BusinessProfile profile;
        try {
            var constructor = BusinessProfile.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            profile = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("BusinessProfile 인스턴스 생성 실패", e);
        }
        ReflectionTestUtils.setField(profile, "businessNumber", businessNumber);
        return profile;
    }
}
