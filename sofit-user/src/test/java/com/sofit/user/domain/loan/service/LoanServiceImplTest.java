package com.sofit.user.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;

@ExtendWith(MockitoExtension.class)
class LoanServiceImplTest {

    @InjectMocks
    private LoanServiceImpl loanService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanDecisionRepository loanDecisionRepository;

    private static final Long USER_ID = 1L;
    private static final Long APPLICATION_ID = 100L;

    @Test
    @DisplayName("심사 중 대출 목록을 조회해 Item으로 변환한다")
    void findUnderReviewLoans_returnsMappedItems() {
        // given
        LoanApplication application = createApplication(ApplicationStatus.SUBMITTED);
        given(loanApplicationRepository.findByUser_UserIdAndStatusIn(eq(USER_ID), any()))
                .willReturn(List.of(application));

        // when
        LoanApplicationListResponse response = loanService.findUnderReviewLoans(USER_ID);

        // then
        assertThat(response.getLoanApplications()).hasSize(1);
        assertThat(response.getLoanApplications().get(0).getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.getLoanApplications().get(0).getProductName()).isEqualTo("우리 사업자 대출");
    }

    @Test
    @DisplayName("대출 상세 조회 성공 시 상세 응답을 반환한다")
    void findLoanDetail_found_returnsDetail() {
        // given
        LoanApplication application = createApplication(ApplicationStatus.SUBMITTED);
        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(application));

        // when
        LoanApplicationDetailResponse response = loanService.findLoanDetail(USER_ID, APPLICATION_ID);

        // then
        assertThat(response.getApplicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.getRequestedAmount()).isEqualTo(10_000_000L);
    }

    @Test
    @DisplayName("대출 상세 조회 시 신청 건이 없으면 APPLICATION_NOT_FOUND 예외를 던진다")
    void findLoanDetail_notFound_throws() {
        // given
        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanService.findLoanDetail(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("심사 완료 대출 목록을 조회해 Item으로 변환한다")
    void findCompletedLoans_returnsMappedItems() {
        // given
        LoanApplication application = createApplication(ApplicationStatus.APPROVED);
        given(loanApplicationRepository.findCompletedByUserIdWithProduct(eq(USER_ID), any()))
                .willReturn(List.of(application));

        // when
        CompletedLoanListResponse response = loanService.findCompletedLoans(USER_ID);

        // then
        assertThat(response.loanApplications()).hasSize(1);
        assertThat(response.loanApplications().get(0).applicationId()).isEqualTo(APPLICATION_ID);
    }

    @Test
    @DisplayName("심사 완료 상세 조회 성공 시 결정 정보를 포함해 반환한다")
    void findCompletedLoanDetail_success_returnsDetailWithDecision() {
        // given
        LoanApplication application = createApplication(ApplicationStatus.APPROVED);
        LoanDecision decision = createDecision(application);
        given(loanApplicationRepository.findCompletedDetailByApplicationIdAndUserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(application));
        given(loanDecisionRepository.findByApplication_ApplicationIdAndStatusIn(eq(APPLICATION_ID), any()))
                .willReturn(Optional.of(decision));

        // when
        CompletedLoanDetailResponse response = loanService.findCompletedLoanDetail(USER_ID, APPLICATION_ID);

        // then
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.decisionInfo().decision()).isEqualTo(DecisionStatus.SYSTEM_APPROVED);
        assertThat(response.decisionInfo().approvedAmount()).isEqualTo(9_000_000L);
    }

    @Test
    @DisplayName("심사 완료 상세 조회 시 신청 건이 없으면 APPLICATION_NOT_FOUND 예외를 던진다")
    void findCompletedLoanDetail_applicationNotFound_throws() {
        // given
        given(loanApplicationRepository.findCompletedDetailByApplicationIdAndUserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanService.findCompletedLoanDetail(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("심사 완료 상세 조회 시 상태가 완료가 아니면 APPLICATION_NOT_FOUND 예외를 던진다")
    void findCompletedLoanDetail_notCompletedStatus_throws() {
        // given
        LoanApplication application = createApplication(ApplicationStatus.SUBMITTED);
        given(loanApplicationRepository.findCompletedDetailByApplicationIdAndUserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanService.findCompletedLoanDetail(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("심사 완료 상세 조회 시 결정 정보가 없으면 LOAN_DECISION_NOT_FOUND 예외를 던진다")
    void findCompletedLoanDetail_decisionNotFound_throws() {
        // given
        LoanApplication application = createApplication(ApplicationStatus.APPROVED);
        given(loanApplicationRepository.findCompletedDetailByApplicationIdAndUserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(application));
        given(loanDecisionRepository.findByApplication_ApplicationIdAndStatusIn(eq(APPLICATION_ID), any()))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanService.findCompletedLoanDetail(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.LOAN_DECISION_NOT_FOUND);
    }

    private LoanApplication createApplication(ApplicationStatus status) {
        try {
            LoanProduct product = newInstance(LoanProduct.class);
            setField(product, "productName", "우리 사업자 대출");

            LoanApplication application = newInstance(LoanApplication.class);
            setField(application, "applicationId", APPLICATION_ID);
            setField(application, "product", product);
            setField(application, "status", status);
            setField(application, "requestedAmount", 10_000_000L);
            setField(application, "requestedTerm", 12);
            setField(application, "repaymentMethod", RepaymentMethod.EQUAL_PAYMENT);
            setField(application, "appliedAt", LocalDateTime.of(2026, 1, 1, 10, 0));
            setField(application, "updatedAt", LocalDateTime.of(2026, 1, 2, 10, 0));
            return application;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private LoanDecision createDecision(LoanApplication application) {
        try {
            LoanDecision decision = newInstance(LoanDecision.class);
            setField(decision, "application", application);
            setField(decision, "status", DecisionStatus.SYSTEM_APPROVED);
            setField(decision, "approvedAmount", 9_000_000L);
            setField(decision, "approvedRate", new BigDecimal("5.50"));
            setField(decision, "approvedTerm", 12);
            setField(decision, "comment", "승인되었습니다");
            return decision;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private <T> T newInstance(Class<T> clazz) throws Exception {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없습니다: " + fieldName);
    }
}
