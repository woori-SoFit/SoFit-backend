package com.sofit.user.domain.loan.service;

import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;

public interface LoanExecutionService {

    LoanExecutionResultResponse findExecutionResult(Long userId, Long applicationId);
}
