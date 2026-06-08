package com.sofit.user.domain.loan.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;

public record CompletedLoanDetailResponse(
        Long applicationId,
        String productName,
        Long requestedAmount,
        RepaymentMethod repaymentMethod,
        DecisionInfo decisionInfo
) {

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record DecisionInfo(
            DecisionStatus decision,
            Long approvedAmount,
            BigDecimal approvedRate,
            Integer approvedTerm,
            String comment
    ) {
    }
}
