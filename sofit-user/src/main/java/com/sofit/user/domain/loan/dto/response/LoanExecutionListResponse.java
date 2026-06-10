package com.sofit.user.domain.loan.dto.response;

import java.util.List;

public record LoanExecutionListResponse(
        List<LoanExecutionItemResponse> executions
) {}
