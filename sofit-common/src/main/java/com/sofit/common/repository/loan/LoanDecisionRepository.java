package com.sofit.common.repository.loan;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.enums.DecisionStatus;

public interface LoanDecisionRepository extends JpaRepository<LoanDecision, Long> {

    List<LoanDecision> findAllByApplication_ApplicationIdOrderByCreatedAtAsc(Long applicationId);

    /**
     * 특정 상태로 조회 (ex: SYSTEM_APPROVED, MANAGER_APPROVED)
     */
    Optional<LoanDecision> findByApplication_ApplicationIdAndStatus(Long applicationId, DecisionStatus status);

    /**
     * 최종 결정 조회 (MANAGER_APPROVED, MANAGER_REJECTED, TELLER_REJECTED 중 하나)
     */
    Optional<LoanDecision> findByApplication_ApplicationIdAndStatusIn(Long applicationId, List<DecisionStatus> statuses);
}
