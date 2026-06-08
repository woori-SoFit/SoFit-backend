package com.sofit.admin.global.batch;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanDecisionTaskletTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanDecisionProcessor loanDecisionProcessor;

    @InjectMocks
    private LoanDecisionTasklet loanDecisionTasklet;

    @Test
    @DisplayName("SUBMITTED 상태의 대출 신청 건이 있으면 Processor를 호출한다")
    void execute_callsProcessorForEachApplication() throws Exception {
        // given
        LoanApplication app1 = mockApplication(201L);
        LoanApplication app2 = mockApplication(202L);

        when(loanApplicationRepository.findByStatus(ApplicationStatus.SUBMITTED))
                .thenReturn(List.of(app1, app2));

        // when
        RepeatStatus result = loanDecisionTasklet.execute(null, null);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(loanDecisionProcessor).processApplication(app1);
        verify(loanDecisionProcessor).processApplication(app2);
    }

    @Test
    @DisplayName("SUBMITTED 대출 신청 건이 없으면 Processor를 호출하지 않는다")

        // LoanApplication 상태 변경 검증
        verify(loanApplicationRepository).save(application);
        verify(application).updateStatus(ApplicationStatus.SYSTEM_APPROVED);

        // LoanDecision 생성 검증
        ArgumentCaptor<LoanDecision> captor = ArgumentCaptor.forClass(LoanDecision.class);
        verify(loanDecisionRepository).save(captor.capture());

        LoanDecision savedDecision = captor.getValue();
        assertThat(savedDecision.getStatus()).isEqualTo(DecisionStatus.SYSTEM_APPROVED);
        assertThat(savedDecision.getApprovedAmount()).isEqualTo(50_000_000L);
        assertThat(savedDecision.getApprovedRate()).isEqualByComparingTo(new BigDecimal("9.00"));
        assertThat(savedDecision.getApprovedTerm()).isEqualTo(12);
        assertThat(savedDecision.getRepaymentMethod()).isEqualTo(RepaymentMethod.EQUAL_PAYMENT);
        assertThat(savedDecision.getComment()).isNull();
    }

    @Test
    @DisplayName("정상 승인 - 요청 금액이 한도 초과 시 한도로 조정")
    void execute_approvedWithAmountCappedToMaxLimit() throws Exception {
        // given: 요청 금액 80,000,000 > max_limit 70,000,000
        LoanApplication highAmountApp = mockApplication(101L, product, 80_000_000L, 24, RepaymentMethod.BULLET);

        when(loanApplicationRepository.findByStatus(ApplicationStatus.S_COMPLETED))
                .thenReturn(List.of(highAmountApp));
        when(scbRepository.findByApplicationId(101L))
                .thenReturn(Optional.of(scb));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, 760))
                .thenReturn(Optional.of(ratePolicy));

        // when
        loanDecisionTasklet.execute(null, null);

        // then
        ArgumentCaptor<LoanDecision> captor = ArgumentCaptor.forClass(LoanDecision.class);
        verify(loanDecisionRepository).save(captor.capture());

        LoanDecision savedDecision = captor.getValue();
        assertThat(savedDecision.getStatus()).isEqualTo(DecisionStatus.SYSTEM_APPROVED);
        assertThat(savedDecision.getApprovedAmount()).isEqualTo(70_000_000L); // max_limit으로 조정
        assertThat(savedDecision.getApprovedTerm()).isEqualTo(24);
        assertThat(savedDecision.getRepaymentMethod()).isEqualTo(RepaymentMethod.BULLET);
    }

    @Test
    @DisplayName("거절 - 금리 정책 구간에 해당하지 않는 경우")
    void execute_rejectedWhenNoPolicyMatched() throws Exception {
        // given
        when(loanApplicationRepository.findByStatus(ApplicationStatus.S_COMPLETED))
                .thenReturn(List.of(application));
        when(scbRepository.findByApplicationId(100L))
                .thenReturn(Optional.of(scb));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, 760))
                .thenReturn(Optional.empty()); // 매칭 실패

        // when
        loanDecisionTasklet.execute(null, null);

        // then
        verify(loanApplicationRepository).save(application);
        verify(application).updateStatus(ApplicationStatus.SYSTEM_REJECTED);

        ArgumentCaptor<LoanDecision> captor = ArgumentCaptor.forClass(LoanDecision.class);
        verify(loanDecisionRepository).save(captor.capture());

        LoanDecision savedDecision = captor.getValue();
        assertThat(savedDecision.getStatus()).isEqualTo(DecisionStatus.SYSTEM_REJECTED);
        assertThat(savedDecision.getComment()).isEqualTo("SCB 최소 등급 미달");
        assertThat(savedDecision.getApprovedAmount()).isNull();
        assertThat(savedDecision.getApprovedRate()).isNull();
        assertThat(savedDecision.getRepaymentMethod()).isNull();
    }

    @Test
    @DisplayName("거절 - SCB 데이터가 없는 경우")
    void execute_rejectedWhenScbNotFound() throws Exception {
        // given
        when(loanApplicationRepository.findByStatus(ApplicationStatus.S_COMPLETED))
                .thenReturn(List.of(application));
        when(scbRepository.findByApplicationId(100L))
                .thenReturn(Optional.empty()); // SCB 없음

        // when
        loanDecisionTasklet.execute(null, null);

        // then
        verify(loanApplicationRepository).save(application);
        verify(application).updateStatus(ApplicationStatus.SYSTEM_REJECTED);

        ArgumentCaptor<LoanDecision> captor = ArgumentCaptor.forClass(LoanDecision.class);
        verify(loanDecisionRepository).save(captor.capture());

        LoanDecision savedDecision = captor.getValue();
        assertThat(savedDecision.getStatus()).isEqualTo(DecisionStatus.SYSTEM_REJECTED);
        assertThat(savedDecision.getComment()).isEqualTo("SCB 최소 등급 미달");
    }

    @Test
    @DisplayName("S_COMPLETED 대출 신청 건이 없으면 아무 작업도 하지 않는다")
    void execute_noApplications() throws Exception {
        // given
        when(loanApplicationRepository.findByStatus(ApplicationStatus.SUBMITTED))
                .thenReturn(List.of());

        // when
        RepeatStatus result = loanDecisionTasklet.execute(null, null);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(loanDecisionProcessor, never()).processApplication(any());
    }

    @Test
    @DisplayName("Processor에서 예외 발생 시 다른 건은 계속 처리한다")
    void execute_continuesOnProcessorException() throws Exception {
        // given
        LoanApplication app1 = mockApplication(301L);
        LoanApplication app2 = mockApplication(302L);

        when(loanApplicationRepository.findByStatus(ApplicationStatus.SUBMITTED))
                .thenReturn(List.of(app1, app2));
        doThrow(new RuntimeException("처리 실패")).when(loanDecisionProcessor).processApplication(app1);

        // when
        RepeatStatus result = loanDecisionTasklet.execute(null, null);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(loanDecisionProcessor).processApplication(app1);
        verify(loanDecisionProcessor).processApplication(app2);
    }

    // === 테스트 헬퍼 메서드 ===

    private LoanApplication mockApplication(Long applicationId) {
        LoanProduct product = mock(LoanProduct.class);
        lenient().when(product.getProductId()).thenReturn(1L);

        LoanApplication app = mock(LoanApplication.class);
        lenient().when(app.getApplicationId()).thenReturn(applicationId);
        lenient().when(app.getProduct()).thenReturn(product);
        lenient().when(app.getRequestedAmount()).thenReturn(50_000_000L);
        lenient().when(app.getRequestedTerm()).thenReturn(12);
        lenient().when(app.getRepaymentMethod()).thenReturn(RepaymentMethod.EQUAL_PAYMENT);
        lenient().when(app.getStatus()).thenReturn(ApplicationStatus.SUBMITTED);
        return app;
    }
}
