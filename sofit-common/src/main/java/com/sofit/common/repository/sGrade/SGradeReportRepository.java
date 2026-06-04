package com.sofit.common.repository.sGrade;

import com.sofit.common.entity.sGrade.SGradeReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SGradeReportRepository extends JpaRepository<SGradeReport, Long> {

    /**
     * 특정 사용자의 최신 성장 S등급 결과를 조회한다.
     */
    Optional<SGradeReport> findTopByUser_UserIdOrderByCreatedAtDesc(Long userId);
}
