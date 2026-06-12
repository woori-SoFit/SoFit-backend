package com.sofit.common.repository.sGrade;

import com.sofit.common.entity.sGrade.SGradeReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SGradeReportRepository extends JpaRepository<SGradeReport, Long> {

    /**
     * 특정 사용자의 최신 COMPLETED 상태 성장 S등급 결과를 조회한다.
     * s_grade_history와 JOIN하여 status = COMPLETED인 건만 대상으로 한다.
     */
    @Query("SELECT r FROM SGradeReport r " +
           "JOIN SGradeHistory h ON h.sGradeId = r.sGradeId " +
           "WHERE r.user.userId = :userId " +
           "AND h.status = com.sofit.common.entity.sGrade.enums.SGradeStatus.COMPLETED " +
           "ORDER BY h.evaluatedAt DESC " +
           "LIMIT 1")
    Optional<SGradeReport> findLatestCompletedByUserId(@Param("userId") Long userId);

    /**
     * @deprecated COMPLETED 조건 없이 조회하므로 findLatestCompletedByUserId 사용 권장
     */
    @Deprecated
    Optional<SGradeReport> findTopByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
