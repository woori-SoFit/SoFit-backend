package com.sofit.user.domain.loan.service;

import com.sofit.user.domain.loan.dto.request.AccountVerificationConfirmRequest;
import com.sofit.user.domain.loan.dto.request.AccountVerificationRequest;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionListResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;

public interface LoanExecutionService {

    LoanExecutionResultResponse findExecutionResult(Long userId, Long applicationId);

    LoanExecutionListResponse findExecutionList(Long userId);

    AccountVerificationResponse requestAccountVerification(Long userId, Long applicationId, AccountVerificationRequest request);

    AccountVerificationConfirmResponse confirmAccountVerification(Long userId, Long applicationId, AccountVerificationConfirmRequest request);
}
