package com.sofit.common.repository;

import com.sofit.common.entity.report.ShapExplanation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShapExplanationRepository extends JpaRepository<ShapExplanation, Long> {

    /**
     * 특정 사용자의 최신 성장 S등급 결과를 조회한다.
     */
    Optional<ShapExplanation> findTopByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
