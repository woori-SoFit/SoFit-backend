package com.sofit.admin.domain.loan.dto.response;

public record LoanApplicationDetailResponse(
        Long applicationId,
        String applicantName,
        String businessName,
        String productName,
        String status,
        String appliedAt,
        Long assignedBankerId,
        String assigneeName
) {
}
