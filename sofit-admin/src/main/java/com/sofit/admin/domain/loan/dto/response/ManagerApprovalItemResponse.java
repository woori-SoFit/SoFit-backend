package com.sofit.admin.domain.loan.dto.response;

public record ManagerApprovalItemResponse(
        Long id,
        String applicationDate,
        String applicantName,
        String businessName,
        String productName,
        String requestedByName,
        Long requestedAmount
) {
}
