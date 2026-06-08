package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.client.NotificationPushClient;
import com.sofit.admin.domain.loan.dto.request.LoanApproveRequest;
import com.sofit.admin.domain.loan.dto.request.LoanRejectRequest;
import com.sofit.admin.domain.loan.dto.response.LoanDecisionResponse;
import com.sofit.admin.domain.loan.exception.LoanDecisionErrorCode;
import com.sofit.admin.global.util.AdminRoleService;
import com.sofit.admin.global.util.SecurityUtil;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.notification.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LoanDecisionServiceImpl 단위 테스트")
class LoanDecisionServiceImplTest {

    @InjectMocks
    private LoanDecisionServiceImpl loanDecisionService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanDecisionRepository loanDecisionRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPushClient notificationPushClient;

    @Mock
    private AdminRoleService adminRoleService;

    private MockedStatic<SecurityUtil> securityUtilMock;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    private LoanApproveRequest createApproveRequest() {
        LoanApproveRequest request = mock(LoanApproveRequest.class);
        given(request.getApprovedAmount()).willReturn(50_000_000L);
        given(request.getApprovedRate()).willReturn(new BigDecimal("3.5"));
        given(request.getApprovedTerm()).willReturn(36);
        given(request.getRepaymentMethod()).willReturn(RepaymentMethod.EQUAL_PAYMENT);
        given(request.getComment()).willReturn("승인합니다.");
        return request;
    }

    private LoanRejectRequest createRejectRequest() {
        LoanRejectRequest request = mock(LoanRejectRequest.class);
        given(request.getComment()).willReturn("신용도 부족으로 거절합니다.");
        return request;
    }

    @Nested
    @DisplayName("approveLoanApplication")
    class ApproveLoanApplicationTest {

        @Test
        @DisplayName("존재하지 않는 applicationId로 승인 시 APPLICATION_NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenApplicationNotExists() {
            // given
            given(loanApplicationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(999L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 승인된 건에 대해 승인 시 ALREADY_DECIDED 예외를 던진다")
        void shouldThrowAlreadyDecidedWhenStatusIsApproved() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.APPROVED);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.ALREADY_DECIDED);
        }

        @Test
        @DisplayName("이미 거절된 건에 대해 승인 시 ALREADY_DECIDED 예외를 던진다")
        void shouldThrowAlreadyDecidedWhenStatusIsRejected() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.REJECTED);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.ALREADY_DECIDED);
        }

        @Test
        @DisplayName("TELLER 역할에서 본인에게 배정되지 않은 건에 대해 승인 시 NOT_ASSIGNED_TO_ME 예외를 던진다")
        void shouldThrowNotAssignedWhenDifferentBanker() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(999L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NOT_ASSIGNED_TO_ME);
        }

        @Test
        @DisplayName("SYSTEM_APPROVED 상태에서 ADMIN_BANK_TELLER 권한이 없으면 NO_DECISION_AUTHORITY 예외를 던진다")
        void shouldThrowNoAuthorityWhenNotTeller() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NO_DECISION_AUTHORITY);
        }

        @Test
        @DisplayName("TELLER 역할에서 assignedBankerId가 null이면 NOT_ASSIGNED_TO_ME 예외를 던진다")
        void shouldThrowNotAssignedWhenBankerIdIsNull() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(null);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NOT_ASSIGNED_TO_ME);
        }

        @Test
        @DisplayName("MANAGER_REVIEW 상태에서 ADMIN_BANK_MANAGER 권한이 없으면 NO_DECISION_AUTHORITY 예외를 던진다")
        void shouldThrowNoAuthorityWhenNotManager() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.MANAGER_REVIEW);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            // TELLER 역할이면 MANAGER가 아니므로 배정 검증을 수행함
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(false);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_MANAGER")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NO_DECISION_AUTHORITY);
        }

        @Test
        @DisplayName("MANAGER_REVIEW 상태에서 ADMIN_BANK_MANAGER 권한이 있으면 최종 승인한다")
        void shouldApproveSuccessfullyForManagerReview() {
            // given
            User user = mock(User.class);
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductName()).willReturn("소상공인 대출");

            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.MANAGER_REVIEW);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(app.getApplicationId()).willReturn(1L);
            given(app.getUser()).willReturn(user);
            given(app.getProduct()).willReturn(product);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_MANAGER);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_MANAGER")).thenReturn(true);

            LoanDecision savedDecision = mock(LoanDecision.class);
            given(savedDecision.getDecisionId()).willReturn(3L);
            given(savedDecision.getApplication()).willReturn(app);
            given(savedDecision.getStatus()).willReturn(DecisionStatus.MANAGER_APPROVED);
            given(loanDecisionRepository.save(any(LoanDecision.class))).willReturn(savedDecision);

            LoanApproveRequest request = createApproveRequest();

            // when
            LoanDecisionResponse response = loanDecisionService.approveLoanApplication(1L, request);

            // then
            assertThat(response.decision()).isEqualTo(DecisionStatus.MANAGER_APPROVED);
            verify(app).updateStatus(ApplicationStatus.APPROVED);
        }

        @Test
        @DisplayName("심사 불가 상태(SUBMITTED)에서 승인 시 NOT_DECIDABLE_STATUS 예외를 던진다")
        void shouldThrowNotDecidableStatusWhenInvalidStatus() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SUBMITTED);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NOT_DECIDABLE_STATUS);
        }

        @Test
        @DisplayName("TELLER 정상 승인 시 LoanDecision을 저장하고 상태를 MANAGER_REVIEW로 변경한다")
        void shouldApproveSuccessfullyAsTeller() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(app.getApplicationId()).willReturn(1L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(true);

            LoanDecision savedDecision = mock(LoanDecision.class);
            given(savedDecision.getDecisionId()).willReturn(1L);
            given(savedDecision.getApplication()).willReturn(app);
            given(savedDecision.getStatus()).willReturn(DecisionStatus.TELLER_APPROVED);
            given(loanDecisionRepository.save(any(LoanDecision.class))).willReturn(savedDecision);

            LoanApproveRequest request = createApproveRequest();

            // when
            LoanDecisionResponse response = loanDecisionService.approveLoanApplication(1L, request);

            // then
            assertThat(response.decision()).isEqualTo(DecisionStatus.TELLER_APPROVED);
            verify(loanDecisionRepository).save(any(LoanDecision.class));
            verify(app).updateStatus(ApplicationStatus.MANAGER_REVIEW);
        }

        @Test
        @DisplayName("SYSTEM_REJECTED 상태에서 승인 시 NOT_DECIDABLE_STATUS 예외를 던진다")
        void shouldThrowNotDecidableStatusWhenSystemRejected() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_REJECTED);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NOT_DECIDABLE_STATUS);
        }

        @Test
        @DisplayName("MANAGER 역할은 배정과 무관하게 승인할 수 있다")
        void shouldAllowManagerToApproveWithoutAssignment() {
            // given
            User user = mock(User.class);
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductName()).willReturn("소상공인 대출");

            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.MANAGER_REVIEW);
            given(app.getAssignedBankerId()).willReturn(999L); // 다른 사람에게 배정됨
            given(app.getApplicationId()).willReturn(1L);
            given(app.getUser()).willReturn(user);
            given(app.getProduct()).willReturn(product);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(200L); // 배정자와 다른 ID
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_MANAGER);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_MANAGER")).thenReturn(true);

            LoanDecision savedDecision = mock(LoanDecision.class);
            given(savedDecision.getDecisionId()).willReturn(4L);
            given(savedDecision.getApplication()).willReturn(app);
            given(savedDecision.getStatus()).willReturn(DecisionStatus.MANAGER_APPROVED);
            given(loanDecisionRepository.save(any(LoanDecision.class))).willReturn(savedDecision);

            LoanApproveRequest request = createApproveRequest();

            // when
            LoanDecisionResponse response = loanDecisionService.approveLoanApplication(1L, request);

            // then
            assertThat(response.decision()).isEqualTo(DecisionStatus.MANAGER_APPROVED);
            verify(app).updateStatus(ApplicationStatus.APPROVED);
        }

        @Test
        @DisplayName("MANAGER가 SYSTEM_APPROVED 상태의 건을 처리하면 TELLER 권한으로 판별된다")
        void shouldThrowNoAuthorityWhenManagerTriesSystemApprovedWithoutTellerRole() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_MANAGER);
            // MANAGER 역할이므로 배정 검증 건너뜀
            // 하지만 SYSTEM_APPROVED는 TELLER 권한 필요
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NO_DECISION_AUTHORITY);
        }

        @Test
        @DisplayName("EXECUTED 상태에서 승인 시 NOT_DECIDABLE_STATUS 예외를 던진다")
        void shouldThrowNotDecidableStatusWhenExecuted() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.EXECUTED);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NOT_DECIDABLE_STATUS);
        }
    }

    @Nested
    @DisplayName("rejectLoanApplication")
    class RejectLoanApplicationTest {

        @Test
        @DisplayName("존재하지 않는 applicationId로 거절 시 APPLICATION_NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenApplicationNotExists() {
            // given
            given(loanApplicationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanDecisionService.rejectLoanApplication(999L, createRejectRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.APPLICATION_NOT_FOUND);
        }

        @Test
        @DisplayName("TELLER 정상 거절 시 LoanDecision을 저장하고 상태를 REJECTED로 변경한다")
        void shouldRejectSuccessfullyAsTeller() {
            // given
            User user = mock(User.class);
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductName()).willReturn("소상공인 대출");

            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(app.getApplicationId()).willReturn(1L);
            given(app.getUser()).willReturn(user);
            given(app.getProduct()).willReturn(product);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_TELLER);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(true);

            LoanDecision savedDecision = mock(LoanDecision.class);
            given(savedDecision.getDecisionId()).willReturn(2L);
            given(savedDecision.getApplication()).willReturn(app);
            given(savedDecision.getStatus()).willReturn(DecisionStatus.TELLER_REJECTED);
            given(loanDecisionRepository.save(any(LoanDecision.class))).willReturn(savedDecision);

            LoanRejectRequest request = createRejectRequest();

            // when
            LoanDecisionResponse response = loanDecisionService.rejectLoanApplication(1L, request);

            // then
            assertThat(response.decision()).isEqualTo(DecisionStatus.TELLER_REJECTED);
            verify(loanDecisionRepository).save(any(LoanDecision.class));
            verify(app).updateStatus(ApplicationStatus.REJECTED);
        }

        @Test
        @DisplayName("MANAGER 정상 거절 시 MANAGER_REJECTED로 저장하고 상태를 REJECTED로 변경한다")
        void shouldRejectSuccessfullyAsManager() {
            // given
            User user = mock(User.class);
            LoanProduct product = mock(LoanProduct.class);
            given(product.getProductName()).willReturn("소상공인 대출");

            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.MANAGER_REVIEW);
            given(app.getAssignedBankerId()).willReturn(999L); // 다른 사람에게 배정됨
            given(app.getApplicationId()).willReturn(1L);
            given(app.getUser()).willReturn(user);
            given(app.getProduct()).willReturn(product);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(200L); // 배정자와 다른 ID
            given(adminRoleService.getCurrentUserRole()).willReturn(UserRole.ADMIN_BANK_MANAGER);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_MANAGER")).thenReturn(true);

            LoanDecision savedDecision = mock(LoanDecision.class);
            given(savedDecision.getDecisionId()).willReturn(5L);
            given(savedDecision.getApplication()).willReturn(app);
            given(savedDecision.getStatus()).willReturn(DecisionStatus.MANAGER_REJECTED);
            given(loanDecisionRepository.save(any(LoanDecision.class))).willReturn(savedDecision);

            LoanRejectRequest request = createRejectRequest();

            // when
            LoanDecisionResponse response = loanDecisionService.rejectLoanApplication(1L, request);

            // then
            assertThat(response.decision()).isEqualTo(DecisionStatus.MANAGER_REJECTED);
            verify(loanDecisionRepository).save(any(LoanDecision.class));
            verify(app).updateStatus(ApplicationStatus.REJECTED);
        }
    }
}
