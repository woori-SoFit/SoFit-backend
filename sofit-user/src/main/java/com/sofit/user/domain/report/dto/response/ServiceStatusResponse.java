package com.sofit.user.domain.report.dto.response;

public record ServiceStatusResponse(
        boolean isMybizConnected,
        boolean isSGradeApplied
) {}
