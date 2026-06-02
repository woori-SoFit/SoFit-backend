package com.sofit.admin.global.batch;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanRatePolicy;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.report.Scb;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.LoanDecisionRepository;
import com.sofit.common.repository.LoanRatePolicyRepository;
import com.sofit.common.repository.ScbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanDecisionTasklet implements Tasklet {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ScbRepository scbRepository;
    private final LoanRatePolicyRepository loanRatePolicyRepository;
    private final LoanDecisionRepository loanDecisionRepository;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // 1. S_COMPLETED 상태의 대출 신청 건 조회
        List<LoanApplication> applications =
                loanApplicationRepository.findByStatus(ApplicationStatus.S_COMPLETED);

        log.info("[LoanDecisionBatch] S_COMPLETED 대출 신청 건수: {}", applications.size());

        for (LoanApplication application : applications) {
            processApplication(application);
        }

        log.info("[LoanDecisionBatch] 배치 처리 완료");
        return RepeatStatus.FINISHED;
    }

    private void processApplication(LoanApplication application) {
        Long applicationId = application.getApplicationId();
        Long productId = application.getProduct().getProductId();

        // 2. application_id로 SCB 테이블에서 scb_grade 조회
        Optional<Scb> scbOpt = scbRepository.findByApplicationId(applicationId);
        if (scbOpt.isEmpty()) {
            log.warn("[LoanDecisionBatch] applicationId={} SCB 데이터 없음 → SYSTEM_REJECTED", applicationId);
            rejectApplication(application, "SCB 최소 등급 미달");
            return;
        }

        Integer scbGrade = scbOpt.get().getScbGrade();

        // 3. product_id + scb_grade로 loan_rate_policy 매칭
        Optional<LoanRatePolicy> policyOpt =
                loanRatePolicyRepository.findByProductIdAndScbGrade(productId, scbGrade);

        if (policyOpt.isEmpty()) {
            // 매칭 실패 → SYSTEM_REJECTED
            log.info("[LoanDecisionBatch] applicationId={} 금리 정책 매칭 실패 (scbGrade={}) → SYSTEM_REJECTED",
                    applicationId, scbGrade);
            rejectApplication(application, "SCB 최소 등급 미달");
            return;
        }

        // 4. 매칭 성공 → SYSTEM_APPROVED
        LoanRatePolicy policy = policyOpt.get();
        Long maxLimit = policy.getMaxLimit().longValue();
        Long requestedAmount = application.getRequestedAmount();

        // approved_amount 결정: requested_amount <= max_limit이면 그대로, 아니면 max_limit
        Long approvedAmount = (requestedAmount <= maxLimit) ? requestedAmount : maxLimit;

        application.updateStatus(ApplicationStatus.SYSTEM_APPROVED);
        loanApplicationRepository.save(application);

        LoanDecision decision = LoanDecision.createSystemApproval(
                application,
                approvedAmount,
                policy.getInterestRate(),
                application.getRequestedTerm(),
                application.getRepaymentMethod()
        );
        loanDecisionRepository.save(decision);

        log.info("[LoanDecisionBatch] applicationId={} → SYSTEM_APPROVED (금리={}, 한도={})",
                applicationId, policy.getInterestRate(), approvedAmount);
    }

    private void rejectApplication(LoanApplication application, String rejectionReason) {
        application.updateStatus(ApplicationStatus.SYSTEM_REJECTED);
        loanApplicationRepository.save(application);

        LoanDecision decision = LoanDecision.createSystemRejection(application, rejectionReason);
        loanDecisionRepository.save(decision);
    }
}
