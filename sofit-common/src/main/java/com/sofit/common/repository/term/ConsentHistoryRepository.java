package com.sofit.common.repository.term;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sofit.common.entity.term.ConsentHistory;

public interface ConsentHistoryRepository extends JpaRepository<ConsentHistory, Long> {

    @Query("SELECT ch FROM ConsentHistory ch " +
           "WHERE ch.user.userId = :userId " +
           "AND ch.term.termId IN :termIds " +
           "AND (:applicationId IS NULL AND ch.application IS NULL " +
           "     OR ch.application.applicationId = :applicationId)")
    List<ConsentHistory> findExistingConsents(@Param("userId") Long userId,
                                              @Param("termIds") List<Long> termIds,
                                              @Param("applicationId") Long applicationId);

    List<ConsentHistory> findByUser_UserIdOrderByConsentIdAsc(Long userId);

    @EntityGraph(attributePaths = {"term"})
    List<ConsentHistory> findByUser_UserIdAndApplication_ApplicationIdOrderByConsentIdAsc(Long userId, Long applicationId);
}
