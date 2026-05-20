package com.sofit.user.domain.loan.dto.response;

public record LoanApplicationResumeResponse(
        Long applicationId,
        String resumeStep,
        SavedData savedData
) {

    public record SavedData(
            String annualIncome,
            String creditScore,
            String incomeType,
            String existingLoanAmt,
            Boolean consentsAgreed
    ) {
    }
}
