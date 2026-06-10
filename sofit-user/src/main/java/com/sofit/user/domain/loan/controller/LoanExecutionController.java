package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.request.AccountVerificationConfirmRequest;
import com.sofit.user.domain.loan.dto.request.AccountVerificationRequest;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionListResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;
import com.sofit.user.domain.loan.exception.LoanSuccessCode;
import com.sofit.user.domain.loan.service.LoanExecutionService;
import com.sofit.user.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LoanExecutionController implements LoanExecutionControllerDocs {

    private final LoanExecutionService loanExecutionService;

    @GetMapping("/api/loan-executions")
    @Override
    public ApiResponse<LoanExecutionListResponse> getExecutionList() {
        Long userId = SecurityUtil.getCurrentUserId();
        LoanExecutionListResponse response = loanExecutionService.findExecutionList(userId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_EXECUTION_LIST_OK, response);
    }

    @GetMapping("/api/loan-applications/{applicationId}/execution")
    @Override
    public ApiResponse<LoanExecutionResultResponse> getExecutionResult(
            @PathVariable Long applicationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        LoanExecutionResultResponse response =
                loanExecutionService.findExecutionResult(userId, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_EXECUTION_RESULT_OK, response);
    }

    @PostMapping("/api/loan-applications/{applicationId}/account-verification")
    @Override
    public ApiResponse<AccountVerificationResponse> requestAccountVerification(
            @PathVariable Long applicationId,
            @Valid @RequestBody AccountVerificationRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        AccountVerificationResponse response =
                loanExecutionService.requestAccountVerification(userId, applicationId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.ACCOUNT_VERIFICATION_OK, response);
    }

    @PostMapping("/api/loan-applications/{applicationId}/account-verification/confirm")
    @Override
    public ApiResponse<AccountVerificationConfirmResponse> confirmAccountVerification(
            @PathVariable Long applicationId,
            @Valid @RequestBody AccountVerificationConfirmRequest request) {
        Long userId = SecurityUtil.getCurrentUserId();
        AccountVerificationConfirmResponse response =
                loanExecutionService.confirmAccountVerification(userId, applicationId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.ACCOUNT_VERIFICATION_CONFIRM_OK, response);
    }
}
