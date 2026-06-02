package com.sofit.common.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.loan.LoanDecision;

public interface LoanDecisionRepository extends JpaRepository<LoanDecision, Long> {

    Optional<LoanDecision> findTopByApplication_ApplicationIdOrderByCreatedAtDesc(Long applicationId);

    List<LoanDecision> findAllByApplication_ApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
