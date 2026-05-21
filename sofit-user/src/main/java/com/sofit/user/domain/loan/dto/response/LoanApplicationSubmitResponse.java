package com.sofit.user.domain.loan.dto.response;

import java.time.LocalDateTime;

public record LoanApplicationSubmitResponse(
        Long applicationId,
        String productName,
        Long requestedAmount,
        LocalDateTime appliedAt,
        String repaymentMethod,
        String purpose,
        Integer requestedTerm
) {
}
