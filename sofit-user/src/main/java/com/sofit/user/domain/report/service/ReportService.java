package com.sofit.user.domain.report.service;

import com.sofit.user.domain.report.dto.response.GradeDetailResponse;
import com.sofit.user.domain.report.dto.response.GradeResponse;
import com.sofit.user.domain.report.dto.response.MybizStatusResponse;

public interface ReportService {

    /**
     * 사용자의 최신 성장 S등급 결과를 조회한다.
     */
    GradeResponse findGrade(Long userId);

    /**
     * 사용자의 성장 S등급 상세 리포트를 조회한다.
     */
    GradeDetailResponse findGradeDetail(Long userId);

    /**
     * 사용자의 마이비즈 연동 여부를 조회한다.
     */
    MybizStatusResponse findMybizStatus(Long userId);
}
