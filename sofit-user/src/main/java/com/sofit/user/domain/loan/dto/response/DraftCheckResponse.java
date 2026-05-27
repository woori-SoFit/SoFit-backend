package com.sofit.user.domain.loan.dto.response;

public record DraftCheckResponse(
        boolean hasDraft,
        Long applicationId,
        String lastCompletedStep,
        String resumeStep
) {
}
