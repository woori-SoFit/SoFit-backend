package com.sofit.user.domain.loan.dto.response;

public record DraftItemResponse(
        Long applicationId,
        String productName,
        String lastCompletedStep,
        String resumeStep
) {
}
