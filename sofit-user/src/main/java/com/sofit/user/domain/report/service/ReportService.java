package com.sofit.user.domain.report.service;

import com.sofit.user.domain.report.dto.response.GradeResponse;

public interface ReportService {

    /**
     * 사용자의 최신 성장 S등급 결과를 조회한다.
     */
    GradeResponse findGrade(Long userId);
}
