package com.sofit.common.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofit.common.entity.term.ConsentHistory;

public interface ConsentHistoryRepository extends JpaRepository<ConsentHistory, Long> {

    @Query("SELECT COUNT(ch) > 0 FROM ConsentHistory ch " +
           "WHERE ch.user.userId = :userId " +
           "AND ch.term.termId = :termId " +
           "AND (:applicationId IS NULL AND ch.application IS NULL " +
           "     OR ch.application.applicationId = :applicationId)")
    boolean existsConsent(@Param("userId") Long userId,
                          @Param("termId") Long termId,
                          @Param("applicationId") Long applicationId);
}
