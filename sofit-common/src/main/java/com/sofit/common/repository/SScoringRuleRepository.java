package com.sofit.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.report.SScoringRule;

public interface SScoringRuleRepository extends JpaRepository<SScoringRule, String> {
}
