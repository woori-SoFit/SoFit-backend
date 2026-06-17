package com.sofit.admin.domain.loan.dto.response;

import com.sofit.common.entity.loan.enums.ApplicationStatus;

import java.time.LocalDateTime;

public record LoanApplicationItemResponse(
        Long applicationId,
        LocalDateTime appliedAt,
        String applicantName,
        String businessName,
        String productName,
        ApplicationStatus status,
        Long assignedBankerId,
        String assigneeName,
        Long requestedAmount,
        Long approvedAmount
) {
}
