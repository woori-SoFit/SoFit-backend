package com.sofit.admin.global.batch;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.LoanRatePolicy;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.entity.sGrade.SScoringRule;
import com.sofit.common.entity.sGrade.enums.SGrade;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.loan.LoanRatePolicyRepository;
import com.sofit.common.repository.sGrade.SGradeReportRepository;
import com.sofit.common.repository.sGrade.SScoringRuleRepository;
import com.sofit.common.repository.sGrade.ScbRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanDecisionProcessorTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private ScbRepository scbRepository;

    @Mock
    private LoanRatePolicyRepository loanRatePolicyRepository;

    @Mock
    private LoanDecisionRepository loanDecisionRepository;

    @Mock
    private SGradeReportRepository sGradeReportRepository;

    @Mock
    private SScoringRuleRepository sScoringRuleRepository;

    @Mock
    private CbScoreClient cbScoreClient;

    @InjectMocks
    private LoanDecisionProcessor loanDecisionProcessor;

    // 공통 테스트 데이터
    private LoanApplication application;
    private LoanProduct product;
    private User user;
    private SGradeReport sGradeReport;
    private SScoringRule sScoringRule;
    private LoanRatePolicy ratePolicy;

    @BeforeEach
    void setUp() {
        product = mock(LoanProduct.class);
        lenient().when(product.getProductId()).thenReturn(1L);

        user = mock(User.class);
        lenient().when(user.getUserId()).thenReturn(10L);
        lenient().when(user.getName()).thenReturn("홍길동");
        lenient().when(user.getResidentNumber()).thenReturn("9001011");

        application = mock(LoanApplication.class);
        lenient().when(application.getApplicationId()).thenReturn(100L);
        lenient().when(application.getUser()).thenReturn(user);
        lenient().when(application.getProduct()).thenReturn(product);
        lenient().when(application.getRequestedAmount()).thenReturn(50_000_000L);
        lenient().when(application.getRequestedTerm()).thenReturn(12);
        lenient().when(application.getRepaymentMethod()).thenReturn(RepaymentMethod.EQUAL_PAYMENT);

        sGradeReport = mock(SGradeReport.class);
        lenient().when(sGradeReport.getSGrade()).thenReturn(SGrade.S3);
        lenient().when(sGradeReport.getSGradeId()).thenReturn(1L);

        sScoringRule = mock(SScoringRule.class);
        lenient().when(sScoringRule.getScoreAddition()).thenReturn(60);

        ratePolicy = mock(LoanRatePolicy.class);
        lenient().when(ratePolicy.getInterestRate()).thenReturn(new BigDecimal("9.00"));
        lenient().when(ratePolicy.getMaxLimit()).thenReturn(new BigDecimal("70000000"));
    }

    @Test
    @DisplayName("CB 점수 조회 실패 시 처리를 건너뛴다")
    void processApplication_cbScoreNull_skips() {
        // given
        when(cbScoreClient.getCbScore("홍길동", "9001011")).thenReturn(null);

        // when
        loanDecisionProcessor.processApplication(application);

        // then
        verify(loanDecisionRepository, never()).save(any());
        verify(loanApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("S등급 미산출 시 처리를 건너뛴다")
    void processDecision_noSGradeReport_skips() {
        // given
        when(sGradeReportRepository.findLatestCompletedByUserId(10L)).thenReturn(Optional.empty());

        // when
        loanDecisionProcessor.processDecision(application, 700);

        // then
        verify(loanDecisionRepository, never()).save(any());
        verify(loanApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상 승인 - 요청 금액이 한도 이내인 경우")
    void processDecision_approvedWithinLimit() {
        // given: 요청 금액 50,000,000 <= max_limit 70,000,000
        Integer cbScore = 700;
        Integer scoreAddition = 60;
        Integer scbScore = cbScore + scoreAddition; // 760

        when(sGradeReportRepository.findLatestCompletedByUserId(10L)).thenReturn(Optional.of(sGradeReport));
        when(sScoringRuleRepository.findByGrade("S3")).thenReturn(Optional.of(sScoringRule));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, scbScore)).thenReturn(Optional.of(ratePolicy));

        // when
        loanDecisionProcessor.processDecision(application, cbScore);

        // then: LoanApplication 상태 변경 검증
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
        assertThat(savedDecision.getComment()).isEqualTo("시스템 자동 심사 승인");
    }

    @Test
    @DisplayName("정상 승인 - 요청 금액이 한도 초과 시 한도로 조정")
    void processDecision_approvedWithAmountCappedToMaxLimit() {
        // given: 요청 금액 80,000,000 > max_limit 70,000,000
        LoanApplication highAmountApp = mockApplication(101L, product, 80_000_000L, 24, RepaymentMethod.BULLET);
        Integer cbScore = 700;
        Integer scbScore = cbScore + 60; // 760

        when(sGradeReportRepository.findLatestCompletedByUserId(10L)).thenReturn(Optional.of(sGradeReport));
        when(sScoringRuleRepository.findByGrade("S3")).thenReturn(Optional.of(sScoringRule));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, scbScore)).thenReturn(Optional.of(ratePolicy));

        // when
        loanDecisionProcessor.processDecision(highAmountApp, cbScore);

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
    void processDecision_rejectedWhenNoPolicyMatched() {
        // given
        Integer cbScore = 700;
        Integer scbScore = cbScore + 60; // 760

        when(sGradeReportRepository.findLatestCompletedByUserId(10L)).thenReturn(Optional.of(sGradeReport));
        when(sScoringRuleRepository.findByGrade("S3")).thenReturn(Optional.of(sScoringRule));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, scbScore)).thenReturn(Optional.empty()); // 매칭 실패

        // when
        loanDecisionProcessor.processDecision(application, cbScore);

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
    @DisplayName("전체 흐름 - CB 조회 성공 후 정상 승인까지")
    void processApplication_fullFlowApproved() {
        // given
        Integer cbScore = 700;
        Integer scbScore = cbScore + 60; // 760

        when(cbScoreClient.getCbScore("홍길동", "9001011")).thenReturn(cbScore);
        when(sGradeReportRepository.findLatestCompletedByUserId(10L)).thenReturn(Optional.of(sGradeReport));
        when(sScoringRuleRepository.findByGrade("S3")).thenReturn(Optional.of(sScoringRule));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, scbScore)).thenReturn(Optional.of(ratePolicy));

        // when
        loanDecisionProcessor.processApplication(application);

        // then
        verify(application).updateStatus(ApplicationStatus.SYSTEM_APPROVED);
        verify(loanDecisionRepository).save(any(LoanDecision.class));
    }

    @Test
    @DisplayName("S등급 가산점 규칙이 없으면 가산점 0으로 처리한다")
    void processDecision_noScoringRule_usesZeroAddition() {
        // given: S등급 존재하지만 가산점 규칙 없음 → 가산점 0
        Integer cbScore = 700;
        Integer scbScore = cbScore + 0; // 700 (가산점 0)

        when(sGradeReportRepository.findLatestCompletedByUserId(10L)).thenReturn(Optional.of(sGradeReport));
        when(sScoringRuleRepository.findByGrade("S3")).thenReturn(Optional.empty()); // 규칙 없음
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, scbScore)).thenReturn(Optional.of(ratePolicy));

        // when
        loanDecisionProcessor.processDecision(application, cbScore);

        // then
        verify(scbRepository).save(any());
        verify(application).updateStatus(ApplicationStatus.SYSTEM_APPROVED);
        verify(loanDecisionRepository).save(any(LoanDecision.class));
    }

    @Test
    @DisplayName("요청 금액이 한도와 동일하면 요청 금액 그대로 승인한다")
    void processDecision_approvedWhenRequestedEqualsMaxLimit() {
        // given: 요청 금액 70,000,000 == max_limit 70,000,000
        LoanApplication exactApp = mockApplication(102L, product, 70_000_000L, 12, RepaymentMethod.EQUAL_PAYMENT);
        Integer cbScore = 700;
        Integer scbScore = cbScore + 60; // 760

        when(sGradeReportRepository.findLatestCompletedByUserId(10L)).thenReturn(Optional.of(sGradeReport));
        when(sScoringRuleRepository.findByGrade("S3")).thenReturn(Optional.of(sScoringRule));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, scbScore)).thenReturn(Optional.of(ratePolicy));

        // when
        loanDecisionProcessor.processDecision(exactApp, cbScore);

        // then
        ArgumentCaptor<LoanDecision> captor = ArgumentCaptor.forClass(LoanDecision.class);
        verify(loanDecisionRepository).save(captor.capture());
        LoanDecision savedDecision = captor.getValue();

        assertThat(savedDecision.getApprovedAmount()).isEqualTo(70_000_000L); // 요청금액 == 한도이므로 그대로
    }

    @Test
    @DisplayName("전체 흐름 - CB 조회 성공 후 거절까지")
    void processApplication_fullFlowRejected() {
        // given
        Integer cbScore = 400;
        Integer scbScore = cbScore + 60; // 460

        when(cbScoreClient.getCbScore("홍길동", "9001011")).thenReturn(cbScore);
        when(sGradeReportRepository.findLatestCompletedByUserId(10L)).thenReturn(Optional.of(sGradeReport));
        when(sScoringRuleRepository.findByGrade("S3")).thenReturn(Optional.of(sScoringRule));
        when(loanRatePolicyRepository.findByProductIdAndScbGrade(1L, scbScore)).thenReturn(Optional.empty());

        // when
        loanDecisionProcessor.processApplication(application);

        // then
        verify(application).updateStatus(ApplicationStatus.SYSTEM_REJECTED);
        verify(loanDecisionRepository).save(any(LoanDecision.class));
    }

    // === 테스트 헬퍼 메서드 ===

    private LoanApplication mockApplication(Long applicationId, LoanProduct product,
                                            Long requestedAmount, Integer requestedTerm,
                                            RepaymentMethod repaymentMethod) {
        LoanApplication app = mock(LoanApplication.class);
        lenient().when(app.getApplicationId()).thenReturn(applicationId);
        lenient().when(app.getUser()).thenReturn(user);
        lenient().when(app.getProduct()).thenReturn(product);
        lenient().when(app.getRequestedAmount()).thenReturn(requestedAmount);
        lenient().when(app.getRequestedTerm()).thenReturn(requestedTerm);
        lenient().when(app.getRepaymentMethod()).thenReturn(repaymentMethod);
        return app;
    }
}
