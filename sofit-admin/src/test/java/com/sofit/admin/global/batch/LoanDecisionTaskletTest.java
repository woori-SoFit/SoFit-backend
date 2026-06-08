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
