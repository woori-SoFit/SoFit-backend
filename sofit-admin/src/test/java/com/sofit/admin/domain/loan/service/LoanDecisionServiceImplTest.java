package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.request.LoanApproveRequest;
import com.sofit.admin.domain.loan.dto.request.LoanRejectRequest;
import com.sofit.admin.domain.loan.dto.response.LoanDecisionResponse;
import com.sofit.admin.domain.loan.exception.LoanDecisionErrorCode;
import com.sofit.admin.global.util.SecurityUtil;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.Decision;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.LoanDecisionRepository;
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
import java.util.List;
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
        @DisplayName("본인에게 배정되지 않은 건에 대해 승인 시 NOT_ASSIGNED_TO_ME 예외를 던진다")
        void shouldThrowNotAssignedWhenDifferentBanker() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(999L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);

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
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NO_DECISION_AUTHORITY);
        }

        @Test
        @DisplayName("assignedBankerId가 null이면 NOT_ASSIGNED_TO_ME 예외를 던진다")
        void shouldThrowNotAssignedWhenBankerIdIsNull() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(null);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);

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
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_MANAGER")).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NO_DECISION_AUTHORITY);
        }

        @Test
        @DisplayName("MANAGER_REVIEW 상태에서 ADMIN_BANK_MANAGER 권한이 있으면 정상 승인한다")
        void shouldApproveSuccessfullyForManagerReview() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.MANAGER_REVIEW);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(app.getApplicationId()).willReturn(1L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(false);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_MANAGER")).thenReturn(true);

            LoanDecision savedDecision = mock(LoanDecision.class);
            given(savedDecision.getDecisionId()).willReturn(3L);
            given(savedDecision.getApplication()).willReturn(app);
            given(savedDecision.getDecision()).willReturn(Decision.APPROVED);
            given(loanDecisionRepository.save(any(LoanDecision.class))).willReturn(savedDecision);

            LoanApproveRequest request = createApproveRequest();

            // when
            LoanDecisionResponse response = loanDecisionService.approveLoanApplication(1L, request);

            // then
            assertThat(response.decision()).isEqualTo(Decision.APPROVED);
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

            // when & then
            assertThatThrownBy(() -> loanDecisionService.approveLoanApplication(1L, createApproveRequest()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .isEqualTo(LoanDecisionErrorCode.NOT_DECIDABLE_STATUS);
        }

        @Test
        @DisplayName("정상 승인 시 LoanDecision을 저장하고 상태를 APPROVED로 변경한다")
        void shouldApproveSuccessfully() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(true);

            LoanDecision savedDecision = mock(LoanDecision.class);
            given(savedDecision.getDecisionId()).willReturn(1L);
            given(savedDecision.getApplication()).willReturn(app);
            given(savedDecision.getDecision()).willReturn(Decision.APPROVED);
            given(app.getApplicationId()).willReturn(1L);
            given(loanDecisionRepository.save(any(LoanDecision.class))).willReturn(savedDecision);

            LoanApproveRequest request = createApproveRequest();

            // when
            LoanDecisionResponse response = loanDecisionService.approveLoanApplication(1L, request);

            // then
            assertThat(response.decision()).isEqualTo(Decision.APPROVED);
            verify(loanDecisionRepository).save(any(LoanDecision.class));
            verify(app).updateStatus(ApplicationStatus.APPROVED);
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
        @DisplayName("정상 거절 시 LoanDecision을 저장하고 상태를 REJECTED로 변경한다")
        void shouldRejectSuccessfully() {
            // given
            LoanApplication app = mock(LoanApplication.class);
            given(app.getStatus()).willReturn(ApplicationStatus.SYSTEM_APPROVED);
            given(app.getAssignedBankerId()).willReturn(100L);
            given(loanApplicationRepository.findById(1L)).willReturn(Optional.of(app));

            securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(100L);
            securityUtilMock.when(() -> SecurityUtil.hasAuthority("ADMIN_BANK_TELLER")).thenReturn(true);

            LoanDecision savedDecision = mock(LoanDecision.class);
            given(savedDecision.getDecisionId()).willReturn(2L);
            given(savedDecision.getApplication()).willReturn(app);
            given(savedDecision.getDecision()).willReturn(Decision.REJECTED);
            given(app.getApplicationId()).willReturn(1L);
            given(loanDecisionRepository.save(any(LoanDecision.class))).willReturn(savedDecision);

            LoanRejectRequest request = createRejectRequest();

            // when
            LoanDecisionResponse response = loanDecisionService.rejectLoanApplication(1L, request);

            // then
            assertThat(response.decision()).isEqualTo(Decision.REJECTED);
            verify(loanDecisionRepository).save(any(LoanDecision.class));
            verify(app).updateStatus(ApplicationStatus.REJECTED);
        }
    }
}
