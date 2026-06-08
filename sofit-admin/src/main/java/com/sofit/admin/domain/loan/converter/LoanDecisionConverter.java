package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanDecisionResponse;
import com.sofit.common.entity.loan.LoanDecision;

public class LoanDecisionConverter {

    private LoanDecisionConverter() {
    }

    public static LoanDecisionResponse toLoanDecisionResponse(LoanDecision loanDecision) {
        return new LoanDecisionResponse(
                loanDecision.getDecisionId(),
                loanDecision.getApplication().getApplicationId(),
                loanDecision.getStatus()
        );
    }
}
