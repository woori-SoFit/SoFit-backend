package com.sofit.admin.domain.loan.dto.response;

public record LoanStatisticsResponse(
        int pending,
        int managerReview,
        int approved,
        int rejected
) {
}
