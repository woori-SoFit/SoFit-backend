package com.sofit.admin.global.batch;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanRatePolicy;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.sGrade.Scb;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.loan.LoanRatePolicyRepository;
import com.sofit.common.repository.sGrade.ScbRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 대출 심사 배치에서 건별 트랜잭션 분리를 위한 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoanDecisionProcessor {

    private final LoanApplicationRepository loanApplicationRepository;
    private final ScbRepository scbRepository;
    private final LoanRatePolicyRepository loanRatePolicyRepository;
    private final LoanDecisionRepository loanDecisionRepository;

    @Transactional
    public void processApplication(LoanApplication application) {
        Long applicationId = application.getApplicationId();
        Long productId = application.getProduct().getProductId();

        // SCB 테이블에서 scb_grade 조회
        Optional<Scb> scbOpt = scbRepository.findByApplicationId(applicationId);
        if (scbOpt.isEmpty()) {
            log.warn("[LoanDecisionBatch] applicationId={} SCB 데이터 없음 → SYSTEM_REJECTED", applicationId);
            rejectApplication(application, "SCB 최소 등급 미달");
            return;
        }

        Integer scbGrade = scbOpt.get().getScbScore();

        // product_id + scb_grade로 loan_rate_policy 매칭
        Optional<LoanRatePolicy> policyOpt =
                loanRatePolicyRepository.findByProductIdAndScbGrade(productId, scbGrade);

        if (policyOpt.isEmpty()) {
            log.info("[LoanDecisionBatch] applicationId={} 금리 정책 매칭 실패 (scbGrade={}) → SYSTEM_REJECTED",
                    applicationId, scbGrade);
            rejectApplication(application, "SCB 최소 등급 미달");
            return;
        }

        // 매칭 성공 → SYSTEM_APPROVED
        LoanRatePolicy policy = policyOpt.get();
        BigDecimal maxLimit = policy.getMaxLimit();
        BigDecimal requestedAmount = BigDecimal.valueOf(application.getRequestedAmount());

        // approved_amount 결정: requested_amount <= max_limit이면 그대로, 아니면 max_limit
        Long approvedAmount = (requestedAmount.compareTo(maxLimit) <= 0)
                ? requestedAmount.longValue()
                : maxLimit.longValue();

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

    private void rejectApplication(LoanApplication application, String comment) {
        application.updateStatus(ApplicationStatus.SYSTEM_REJECTED);
        loanApplicationRepository.save(application);

        LoanDecision decision = LoanDecision.createSystemRejection(application, comment);
        loanDecisionRepository.save(decision);
    }
}
