package com.sofit.user.domain.loan.dto.response;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sofit.common.entity.loan.enums.Decision;
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
            Decision decision,
            Long approvedAmount,
            BigDecimal approvedRate,
            Integer approvedTerm,
            String rejectionReason
    ) {
    }
}
