package com.sofit.user.domain.loan.dto.response;

import java.time.LocalDate;
import java.util.List;

import com.sofit.common.entity.loan.enums.ApplicationStatus;

public record CompletedLoanListResponse(
        List<CompletedLoanItem> loanApplications
) {
    public record CompletedLoanItem(
            Long applicationId,
            String productName,
            ApplicationStatus status,
            Long requestedAmount,
            LocalDate appliedAt,
            LocalDate updatedAt
    ) {
    }
}
