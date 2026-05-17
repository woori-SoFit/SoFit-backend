package com.sofit.common.repository;

import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.enums.DecisionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LoanDecisionRepository extends JpaRepository<LoanDecision, Long> {

    Optional<LoanDecision> findByApplication_ApplicationIdAndDecision(
            Long applicationId, DecisionType decision
    );
}
