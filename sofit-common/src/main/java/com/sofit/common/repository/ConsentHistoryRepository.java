package com.sofit.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sofit.common.entity.term.ConsentHistory;

public interface ConsentHistoryRepository extends JpaRepository<ConsentHistory, Long> {

    boolean existsByUserIdAndTermIdAndApplicationId(Long userId, Long termId, Long applicationId);
}
