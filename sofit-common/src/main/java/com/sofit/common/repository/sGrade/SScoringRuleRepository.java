package com.sofit.common.repository.sGrade;

import com.sofit.common.entity.sGrade.SScoringRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SScoringRuleRepository extends JpaRepository<SScoringRule, Long> {

    /**
     * 등급명으로 가산점 규칙을 조회한다.
     */
    Optional<SScoringRule> findByGrade(String grade);
}
