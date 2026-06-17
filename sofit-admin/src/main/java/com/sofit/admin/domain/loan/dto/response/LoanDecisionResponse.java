package com.sofit.admin.domain.loan.dto.response;

import com.sofit.common.entity.loan.enums.DecisionStatus;

public record LoanDecisionResponse(
        Long decisionId,
        Long applicationId,
        DecisionStatus decision
) {
}
