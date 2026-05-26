package com.sofit.user.domain.report.dto.response;

import java.time.LocalDateTime;

public record GradeResponse(
        Long evaluationId,
        Long userId,
        String sGrade,
        String comment,
        String commentDetail,
        LocalDateTime createdAt
) {}
