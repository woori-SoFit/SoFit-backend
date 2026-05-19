package com.sofit.user.domain.loan.dto.response;

import com.sofit.common.entity.loan.enums.RepaymentMethod;

import java.math.BigDecimal;

public record LoanExecutionResultResponse(
        Long executionId,
        Long applicationId,
        Long productId,
        String productName,
        Long executedAmount,
        BigDecimal approvedRate,
        Integer approvedTerm,
        RepaymentMethod repaymentMethod
) {}
