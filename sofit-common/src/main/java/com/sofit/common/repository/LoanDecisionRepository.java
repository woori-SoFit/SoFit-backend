package com.sofit.common.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.loan.LoanDecision;

public interface LoanDecisionRepository extends JpaRepository<LoanDecision, Long> {

    Optional<LoanDecision> findByApplication_ApplicationId(Long applicationId);
}
