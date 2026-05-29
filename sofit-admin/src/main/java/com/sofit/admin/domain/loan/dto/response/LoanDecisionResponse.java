package com.sofit.admin.domain.loan.dto.response;

import com.sofit.common.entity.loan.enums.Decision;

public record LoanDecisionResponse(
        Long decisionId,
        Long applicationId,
        Decision decision
) {
}
