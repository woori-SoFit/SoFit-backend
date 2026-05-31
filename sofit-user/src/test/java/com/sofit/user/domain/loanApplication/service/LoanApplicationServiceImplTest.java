package com.sofit.user.domain.loanApplication.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

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
import com.sofit.common.entity.loan.enums.AnnualIncome;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.CreditScoreRange;
import com.sofit.common.entity.loan.enums.ExistingLoanAmount;
import com.sofit.common.entity.loan.enums.IncomeType;
import com.sofit.common.entity.loan.enums.LastCompletedStep;
import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.ProductStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.LoanProductRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import com.sofit.user.domain.loan.dto.request.LoanApplicationCreateRequest;
import com.sofit.user.domain.loan.dto.request.LoanApplicationSubmitRequest;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.loan.service.BankerAssignmentService;
import com.sofit.user.domain.loan.service.LoanApplicationServiceImpl;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceImplTest {

    @InjectMocks
    private LoanApplicationServiceImpl loanApplicationService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanProductRepository loanProductRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BankerAssignmentService bankerAssignmentService;

    private static final Long USER_ID = 1L;
    private static final Long PRODUCT_ID = 1L;
    private static final Long APPLICATION_ID = 100L;

    // === createApplication ===

    @Test
    @DisplayName("createApplication - 정상적으로 DRAFT 신청을 생성한다")
    void createApplication_success() {
        // given
        User user = createUser(USER_ID);
        LoanProduct product = createActiveProduct(PRODUCT_ID);
        LoanApplicationCreateRequest request = createCreateRequest();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(loanProductRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        given(loanApplicationRepository.existsByUser_UserIdAndProduct_ProductIdAndStatusNot(
                USER_ID, PRODUCT_ID, ApplicationStatus.CANCELLED)).willReturn(false);
        given(loanApplicationRepository.save(any(LoanApplication.class)))
                .willAnswer(invocation -> {
                    LoanApplication app = invocation.getArgument(0);
                    ReflectionTestUtils.setField(app, "applicationId", APPLICATION_ID);
                    return app;
                });

        // when
        LoanApplicationCreateResponse response = loanApplicationService.createApplication(USER_ID, PRODUCT_ID, request);

        // then
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        verify(loanApplicationRepository).save(any(LoanApplication.class));
    }

    @Test
    @DisplayName("createApplication - 사용자 미존재 시 USER_NOT_FOUND 예외")
    void createApplication_throwsException_whenUserNotFound() {
        // given
        LoanApplicationCreateRequest request = createCreateRequest();
        given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanApplicationService.createApplication(USER_ID, PRODUCT_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.USER_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("createApplication - 상품 미존재 시 PRODUCT_NOT_FOUND 예외")
    void createApplication_throwsException_whenProductNotFound() {
        // given
        User user = createUser(USER_ID);
        LoanApplicationCreateRequest request = createCreateRequest();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(loanProductRepository.findById(PRODUCT_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanApplicationService.createApplication(USER_ID, PRODUCT_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.PRODUCT_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("createApplication - 비활성 상품이면 PRODUCT_NOT_ACTIVE 예외")
    void createApplication_throwsException_whenProductNotActive() {
        // given
        User user = createUser(USER_ID);
        LoanProduct product = createInactiveProduct(PRODUCT_ID);
        LoanApplicationCreateRequest request = createCreateRequest();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(loanProductRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));

        // when & then
        assertThatThrownBy(() -> loanApplicationService.createApplication(USER_ID, PRODUCT_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.PRODUCT_NOT_ACTIVE);
                });
    }

    @Test
    @DisplayName("createApplication - 중복 신청 시 DUPLICATE_APPLICATION 예외")
    void createApplication_throwsException_whenDuplicateApplication() {
        // given
        User user = createUser(USER_ID);
        LoanProduct product = createActiveProduct(PRODUCT_ID);
        LoanApplicationCreateRequest request = createCreateRequest();

        given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
        given(loanProductRepository.findById(PRODUCT_ID)).willReturn(Optional.of(product));
        given(loanApplicationRepository.existsByUser_UserIdAndProduct_ProductIdAndStatusNot(
                USER_ID, PRODUCT_ID, ApplicationStatus.CANCELLED)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> loanApplicationService.createApplication(USER_ID, PRODUCT_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.DUPLICATE_APPLICATION);
                });
    }

    // === checkDraft ===

    @Test
    @DisplayName("checkDraft - DRAFT 존재 시 hasDraft=true 반환")
    void checkDraft_returnsDraft_whenExists() {
        // given
        LoanApplication application = createDraftApplication(APPLICATION_ID, USER_ID, PRODUCT_ID);
        given(loanApplicationRepository.findByUser_UserIdAndProduct_ProductIdAndStatus(
                USER_ID, PRODUCT_ID, ApplicationStatus.DRAFT))
                .willReturn(Optional.of(application));

        // when
        DraftCheckResponse response = loanApplicationService.checkDraft(USER_ID, PRODUCT_ID);

        // then
        assertThat(response.hasDraft()).isTrue();
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    @DisplayName("checkDraft - DRAFT 미존재 시 hasDraft=false 반환")
    void checkDraft_returnsNoDraft_whenNotExists() {
        // given
        given(loanApplicationRepository.findByUser_UserIdAndProduct_ProductIdAndStatus(
                USER_ID, PRODUCT_ID, ApplicationStatus.DRAFT))
                .willReturn(Optional.empty());

        // when
        DraftCheckResponse response = loanApplicationService.checkDraft(USER_ID, PRODUCT_ID);

        // then
        assertThat(response.hasDraft()).isFalse();
        assertThat(response.applicationId()).isNull();
    }

    // === getResumeData ===

    @Test
    @DisplayName("getResumeData - DRAFT 상태 신청의 이어가기 데이터를 반환한다")
    void getResumeData_returnsResumeData() {
        // given
        LoanApplication application = createDraftApplication(APPLICATION_ID, USER_ID, PRODUCT_ID);
        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(application));

        // when
        LoanApplicationResumeResponse response = loanApplicationService.getResumeData(USER_ID, APPLICATION_ID);

        // then
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.resumeStep()).isEqualTo("CONSENT");
    }

    @Test
    @DisplayName("getResumeData - 신청 미존재 시 APPLICATION_NOT_FOUND 예외")
    void getResumeData_throwsException_whenNotFound() {
        // given
        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanApplicationService.getResumeData(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("getResumeData - DRAFT가 아닌 상태면 APPLICATION_NOT_FOUND 예외")
    void getResumeData_throwsException_whenNotDraft() {
        // given
        LoanApplication application = createDraftApplication(APPLICATION_ID, USER_ID, PRODUCT_ID);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.SUBMITTED);

        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanApplicationService.getResumeData(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
                });
    }

    // === submitApplication ===

    @Test
    @DisplayName("submitApplication - 정상적으로 제출 처리한다")
    void submitApplication_success() {
        // given
        LoanApplication application = createDraftApplication(APPLICATION_ID, USER_ID, PRODUCT_ID);
        LoanApplicationSubmitRequest request = createSubmitRequest();

        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(application));
        given(bankerAssignmentService.assignBanker()).willReturn(10L);

        // when
        LoanApplicationSubmitResponse response = loanApplicationService.submitApplication(USER_ID, APPLICATION_ID, request);

        // then
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(application.getAssignedBankerId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("submitApplication - 신청 미존재 시 APPLICATION_NOT_FOUND 예외")
    void submitApplication_throwsException_whenNotFound() {
        // given
        LoanApplicationSubmitRequest request = createSubmitRequest();
        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanApplicationService.submitApplication(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
                });
    }

    @Test
    @DisplayName("submitApplication - DRAFT가 아닌 상태면 APPLICATION_NOT_DRAFT 예외")
    void submitApplication_throwsException_whenNotDraft() {
        // given
        LoanApplication application = createDraftApplication(APPLICATION_ID, USER_ID, PRODUCT_ID);
        ReflectionTestUtils.setField(application, "status", ApplicationStatus.SUBMITTED);
        LoanApplicationSubmitRequest request = createSubmitRequest();

        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanApplicationService.submitApplication(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .satisfies(exception -> {
                    BaseException e = (BaseException) exception;
                    assertThat(e.getErrorCode()).isEqualTo(LoanErrorCode.APPLICATION_NOT_DRAFT);
                });
    }

    // --- 헬퍼 메서드 ---

    private User createUser(Long userId) {
        User user = User.createUser("testuser", "hashedpw", "홍길동", "01012345678", "9901011");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private LoanProduct createActiveProduct(Long productId) {
        LoanProduct product;
        try {
            var constructor = LoanProduct.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            product = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("LoanProduct 인스턴스 생성 실패", e);
        }
        ReflectionTestUtils.setField(product, "productId", productId);
        ReflectionTestUtils.setField(product, "productName", "소상공인 성장 대출");
        ReflectionTestUtils.setField(product, "status", ProductStatus.ACTIVE);
        return product;
    }

    private LoanProduct createInactiveProduct(Long productId) {
        LoanProduct product;
        try {
            var constructor = LoanProduct.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            product = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("LoanProduct 인스턴스 생성 실패", e);
        }
        ReflectionTestUtils.setField(product, "productId", productId);
        ReflectionTestUtils.setField(product, "productName", "비활성 상품");
        ReflectionTestUtils.setField(product, "status", ProductStatus.INACTIVE);
        return product;
    }

    private LoanApplication createDraftApplication(Long applicationId, Long userId, Long productId) {
        User user = createUser(userId);
        LoanProduct product = createActiveProduct(productId);
        LoanApplication application = LoanApplication.createDraft(
                user, product,
                AnnualIncome.AMT_30_50M,
                CreditScoreRange.CS_0_850,
                IncomeType.SALARY,
                ExistingLoanAmount.LOAN_0_100M
        );
        ReflectionTestUtils.setField(application, "applicationId", applicationId);
        return application;
    }

    private LoanApplicationCreateRequest createCreateRequest() {
        LoanApplicationCreateRequest request = new LoanApplicationCreateRequest();
        ReflectionTestUtils.setField(request, "annualIncome", AnnualIncome.AMT_30_50M);
        ReflectionTestUtils.setField(request, "creditScore", CreditScoreRange.CS_0_850);
        ReflectionTestUtils.setField(request, "incomeType", IncomeType.SALARY);
        ReflectionTestUtils.setField(request, "existingLoanAmt", ExistingLoanAmount.LOAN_0_100M);
        return request;
    }

    private LoanApplicationSubmitRequest createSubmitRequest() {
        LoanApplicationSubmitRequest request = new LoanApplicationSubmitRequest();
        ReflectionTestUtils.setField(request, "requestedAmount", 30_000_000L);
        ReflectionTestUtils.setField(request, "requestedTerm", 36);
        ReflectionTestUtils.setField(request, "repaymentMethod", RepaymentMethod.EQUAL_PAYMENT);
        ReflectionTestUtils.setField(request, "purpose", LoanPurpose.WORKING_CAPITAL);
        return request;
    }
}
