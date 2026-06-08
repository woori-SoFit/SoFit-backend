package com.sofit.admin.global.batch;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanRatePolicy;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.sGrade.Scb;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.entity.sGrade.SScoringRule;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.loan.LoanRatePolicyRepository;
import com.sofit.common.repository.sGrade.SGradeReportRepository;
import com.sofit.common.repository.sGrade.SScoringRuleRepository;
import com.sofit.common.repository.sGrade.ScbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.math.BigDecimal;
import java.util.Optional;

/**
 * 대출 심사 배치에서 건별 처리를 위한 서비스.
 * 외부 API 호출은 트랜잭션 밖에서, DB 작업은 트랜잭션 안에서 수행한다.
 *
 * 처리 흐름:
 * 1) [트랜잭션 X] 외부 CB 점수 조회
 * 2) [트랜잭션 O] S등급 + 가산점 조회 → SCB 계산 → 심사 결정
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanDecisionProcessor {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ScbRepository scbRepository;
    private final LoanRatePolicyRepository loanRatePolicyRepository;
    private final LoanDecisionRepository loanDecisionRepository;
    private final SGradeReportRepository sGradeReportRepository;
    private final SScoringRuleRepository sScoringRuleRepository;
    private final CbScoreClient cbScoreClient;

    /**
     * 대출 신청 건 처리 (외부 API 호출 → DB 작업 순서).
     * 외부 API 호출은 트랜잭션 밖에서 수행하여 DB 커넥션 점유를 방지한다.
     */
    public void processApplication(LoanApplication application) {
        Long applicationId = application.getApplicationId();

        // 1. [트랜잭션 X] 외부 API에서 CB 점수 조회 (3회 재시도 포함)
        String name = application.getUser().getName();
        String residentNumber = application.getUser().getResidentNumber();
        Integer cbScore = cbScoreClient.getCbScore(name, residentNumber);
        if (cbScore == null) {
            log.warn("[LoanDecisionBatch] applicationId={} CB 점수 조회 실패 → 건너뜀 (다음 배치에서 재시도)", applicationId);
            return;
        }

        // 2. [트랜잭션 O] DB 조회 + 심사 결정 처리
        processDecision(application, cbScore);
    }

    /**
     * DB 작업을 트랜잭션으로 묶어 처리한다.
     * S등급 조회, SCB 계산, 금리 정책 매칭, 심사 결정을 하나의 트랜잭션으로 수행한다.
     */
    @Transactional
    public void processDecision(LoanApplication application, Integer cbScore) {
        Long applicationId = application.getApplicationId();
        Long userId = application.getUser().getUserId();
        Long productId = application.getProduct().getProductId();

        // 1. S등급 조회 (COMPLETED 상태의 최신 s_grade_report)
        Optional<SGradeReport> reportOpt = sGradeReportRepository.findLatestCompletedByUserId(userId);
        if (reportOpt.isEmpty()) {
            log.warn("[LoanDecisionBatch] applicationId={} S등급 미산출 → 건너뜀 (다음 배치에서 재시도)", applicationId);
            return;
        }

        SGradeReport sGradeReport = reportOpt.get();
        String sGradeValue = sGradeReport.getSGrade().name();

        // loan_application에 참조한 s_grade_id 저장
        application.updateSGradeId(sGradeReport.getSGradeId());

        // 2. S등급 기반 가산점 조회 (s_scoring_rule)
        Optional<SScoringRule> ruleOpt = sScoringRuleRepository.findByGrade(sGradeValue);
        Integer scoreAddition = ruleOpt.map(SScoringRule::getScoreAddition).orElse(0);

        // 3. SCB = CB + 가산점 계산 → scb 테이블 INSERT
        Integer scbScore = cbScore + scoreAddition;
        Scb scb = Scb.create(applicationId, cbScore, sGradeValue, scoreAddition, scbScore);
        scbRepository.save(scb);

        // 4. product_id + scb_score로 loan_rate_policy 매칭
        Optional<LoanRatePolicy> policyOpt =
                loanRatePolicyRepository.findByProductIdAndScbGrade(productId, scbScore);

        if (policyOpt.isEmpty()) {
            log.info("[LoanDecisionBatch] applicationId={} 금리 정책 매칭 실패 (scbScore={}) → SYSTEM_REJECTED",
                    applicationId, scbScore);
            rejectApplication(application, "SCB 최소 등급 미달");
            return;
        }

        // 5. 매칭 성공 → SYSTEM_APPROVED
        LoanRatePolicy policy = policyOpt.get();
        BigDecimal maxLimit = policy.getMaxLimit();
        BigDecimal requestedAmount = BigDecimal.valueOf(application.getRequestedAmount());

        // approved_amount 결정: requested_amount <= max_limit이면 그대로, 아니면 max_limit
        Long approvedAmount = (requestedAmount.compareTo(maxLimit) <= 0)
                ? requestedAmount.longValue()
                : maxLimit.longValue();

        application.updateStatus(ApplicationStatus.SYSTEM_APPROVED);
        loanApplicationRepository.save(application);

        LoanDecision decision = LoanDecision.createApproval(
                application,
                DecisionStatus.SYSTEM_APPROVED,
                approvedAmount,
                policy.getInterestRate(),
                application.getRequestedTerm(),
                application.getRepaymentMethod(),
                "시스템 자동 심사 승인",
                null
        );
        loanDecisionRepository.save(decision);

        log.info("[LoanDecisionBatch] applicationId={} → SYSTEM_APPROVED (금리={}, 한도={})",
                applicationId, policy.getInterestRate(), approvedAmount);
    }

    private void rejectApplication(LoanApplication application, String comment) {
        application.updateStatus(ApplicationStatus.SYSTEM_REJECTED);
        loanApplicationRepository.save(application);

        LoanDecision decision = LoanDecision.createRejection(application, DecisionStatus.SYSTEM_REJECTED, comment, null);
        loanDecisionRepository.save(decision);
    }
}
