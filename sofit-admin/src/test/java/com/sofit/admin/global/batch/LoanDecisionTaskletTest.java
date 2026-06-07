package com.sofit.admin.global.batch;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.LoanRatePolicy;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.sGrade.Scb;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.loan.LoanRatePolicyRepository;
import com.sofit.common.repository.sGrade.ScbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanDecisionTaskletTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private ScbRepository scbRepository;

    @Mock
    private LoanRatePolicyRepository loanRatePolicyRepository;

    @Mock
    private LoanDecisionRepository loanDecisionRepository;

    @InjectMocks
    private LoanDecisionTasklet loanDecisionTasklet;

    private LoanApplication application;
    private LoanProduct product;
    private Scb scb;
    private LoanRatePolicy ratePolicy;

    @BeforeEach
    void setUp() {
        product = mockProduct(1L);
        application = mockApplication(100L, product, 50_000_000L, 12, RepaymentMethod.EQUAL_PAYMENT);
        scb = mockScb(100L, 760);
        ratePolicy = mockRatePolicy(product, 750, 780, new BigDecimal("9.00"), new BigDecimal("70000000"));
    }

    @Test
    @DisplayName("정상 승인 - scbGrade가 정책 구간에 해당하고 요청 금액이 한도 이내인 경우")
    void execute_approvedWhenScoreInRangeAndAmountWithinLimit() throws Exception {
        // given
        when(loanApplicationRepository.findByStatus(ApplicationStatus.S_COMPLETED))
                .thenReturn(List.of(application));
        when(scbRepository.findByApplicationId(100L))
                .thenReturn(Optional.of(scb));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, 760))
                .thenReturn(Optional.of(ratePolicy));

        // when
        RepeatStatus result = loanDecisionTasklet.execute(null, null);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);

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
        when(loanApplicationRepository.findByStatus(ApplicationStatus.S_COMPLETED))
                .thenReturn(List.of());

        // when
        RepeatStatus result = loanDecisionTasklet.execute(null, null);

        // then
        assertThat(result).isEqualTo(RepeatStatus.FINISHED);
        verify(scbRepository, never()).findByApplicationId(any());
        verify(loanDecisionRepository, never()).save(any());
    }

    @Test
    @DisplayName("여러 건 처리 - 승인과 거절이 혼합된 경우")
    void execute_multipleApplicationsMixed() throws Exception {
        // given
        LoanApplication app1 = mockApplication(201L, product, 30_000_000L, 12, RepaymentMethod.EQUAL_PRINCIPAL);
        LoanApplication app2 = mockApplication(202L, product, 50_000_000L, 24, RepaymentMethod.EQUAL_PAYMENT);

        Scb scb1 = mockScb(201L, 760); // 정책 구간 내
        Scb scb2 = mockScb(202L, 600); // 정책 구간 밖

        when(loanApplicationRepository.findByStatus(ApplicationStatus.S_COMPLETED))
                .thenReturn(List.of(app1, app2));
        when(scbRepository.findByApplicationId(201L)).thenReturn(Optional.of(scb1));
        when(scbRepository.findByApplicationId(202L)).thenReturn(Optional.of(scb2));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, 760))
                .thenReturn(Optional.of(ratePolicy));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, 600))
                .thenReturn(Optional.empty());

        // when
        loanDecisionTasklet.execute(null, null);

        // then
        verify(app1).updateStatus(ApplicationStatus.SYSTEM_APPROVED);
        verify(app2).updateStatus(ApplicationStatus.SYSTEM_REJECTED);

        verify(loanDecisionRepository, times(2)).save(any(LoanDecision.class));
    }

    // === 테스트 헬퍼 메서드 (Mockito mock 방식) ===

    private LoanProduct mockProduct(Long productId) {
        LoanProduct p = mock(LoanProduct.class);
        lenient().when(p.getProductId()).thenReturn(productId);
        return p;
    }

    private LoanApplication mockApplication(Long applicationId, LoanProduct product,
                                            Long requestedAmount, Integer requestedTerm,
                                            RepaymentMethod repaymentMethod) {
        LoanApplication app = mock(LoanApplication.class);
        lenient().when(app.getApplicationId()).thenReturn(applicationId);
        lenient().when(app.getProduct()).thenReturn(product);
        lenient().when(app.getRequestedAmount()).thenReturn(requestedAmount);
        lenient().when(app.getRequestedTerm()).thenReturn(requestedTerm);
        lenient().when(app.getRepaymentMethod()).thenReturn(repaymentMethod);
        lenient().when(app.getStatus()).thenReturn(ApplicationStatus.S_COMPLETED);
        return app;
    }

    private Scb mockScb(Long applicationId, Integer scbGrade) {
        Scb s = mock(Scb.class);
        lenient().when(s.getApplicationId()).thenReturn(applicationId);
        lenient().when(s.getScbGrade()).thenReturn(scbGrade);
        return s;
    }

    private LoanRatePolicy mockRatePolicy(LoanProduct product, Integer minScore,
                                          Integer maxScore, BigDecimal interestRate,
                                          BigDecimal maxLimit) {
        LoanRatePolicy policy = mock(LoanRatePolicy.class);
        lenient().when(policy.getProduct()).thenReturn(product);
        lenient().when(policy.getMinScore()).thenReturn(minScore);
        lenient().when(policy.getMaxScore()).thenReturn(maxScore);
        lenient().when(policy.getInterestRate()).thenReturn(interestRate);
        lenient().when(policy.getMaxLimit()).thenReturn(maxLimit);
        return policy;
    }
}
