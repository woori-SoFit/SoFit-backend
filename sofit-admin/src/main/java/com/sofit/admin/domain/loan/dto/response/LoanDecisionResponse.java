package com.sofit.admin.domain.loan.dto.response;

public record LoanDecisionResponse(
        Long decisionId,
        Long applicationId,
        String decision
) {
}
