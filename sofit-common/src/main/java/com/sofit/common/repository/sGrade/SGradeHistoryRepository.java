package com.sofit.common.repository.sGrade;

import com.sofit.common.entity.sGrade.SGradeHistory;
import com.sofit.common.entity.sGrade.enums.SGradeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SGradeHistoryRepository extends JpaRepository<SGradeHistory, Long> {

    /**
     * 특정 사용자의 특정 상태인 SGradeHistory 목록을 조회한다.
     * (수동 배치 복구 시 FAILED 상태 조회용)
     */
    List<SGradeHistory> findByUser_UserIdAndStatus(Long userId, SGradeStatus status);
}
