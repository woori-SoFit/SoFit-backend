package com.sofit.user.domain.report.dto.response;

import java.util.List;

public record GradeDetailResponse(
        String sGrade,
        List<String> strengthKeywords,
        List<String> improvementKeywords,
        String advice
) {}
