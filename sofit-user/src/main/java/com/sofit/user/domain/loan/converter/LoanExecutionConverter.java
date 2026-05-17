package com.sofit.user.domain.loan.converter;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanExecution;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;

public class LoanExecutionConverter {

    private LoanExecutionConverter() {}

    public static LoanExecutionResultResponse toResponse(LoanExecution execution, LoanDecision decision) {
        LoanApplication app = execution.getApplication();
        LoanProduct product = app.getProduct();

        return new LoanExecutionResultResponse(
                execution.getExecutionId(),
                app.getApplicationId(),
                product.getProductId(),
                product.getProductName(),
                execution.getExecutionAmount(),
                decision.getApprovedRate(),
                decision.getApprovedTerm(),
                app.getRepaymentMethod()
        );
    }
}
