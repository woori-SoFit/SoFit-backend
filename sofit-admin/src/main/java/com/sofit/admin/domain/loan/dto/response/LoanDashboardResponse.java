package com.sofit.admin.domain.loan.dto.response;

import java.util.List;

public record LoanDashboardResponse(
        long totalCount,
        int totalPages,
        int currentPage,
        int size,
        List<LoanApplicationItemResponse> contents
) {
}
