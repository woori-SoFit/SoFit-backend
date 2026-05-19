package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.request.AccountVerificationConfirmRequest;
import com.sofit.user.domain.loan.dto.request.AccountVerificationRequest;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;
import com.sofit.user.domain.loan.exception.LoanSuccessCode;
import com.sofit.user.domain.loan.service.LoanExecutionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanExecutionController implements LoanExecutionControllerDocs {

    private final LoanExecutionService loanExecutionService;

    // TODO: 세션 인증 구현 후 SecurityContext에서 userId 추출하도록 변경
    private static final Long TEMP_USER_ID = 1L;

    @GetMapping("/{applicationId}/execution")
    @Override
    public ApiResponse<LoanExecutionResultResponse> getExecutionResult(
            @PathVariable Long applicationId) {
        LoanExecutionResultResponse response =
                loanExecutionService.findExecutionResult(TEMP_USER_ID, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_EXECUTION_RESULT_OK, response);
    }

    @PostMapping("/{applicationId}/account-verification")
    @Override
    public ApiResponse<AccountVerificationResponse> requestAccountVerification(
            @PathVariable Long applicationId,
            @Valid @RequestBody AccountVerificationRequest request) {
        AccountVerificationResponse response =
                loanExecutionService.requestAccountVerification(applicationId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.ACCOUNT_VERIFICATION_OK, response);
    }

    @PostMapping("/{applicationId}/account-verification/confirm")
    @Override
    public ApiResponse<AccountVerificationConfirmResponse> confirmAccountVerification(
            @PathVariable Long applicationId,
            @Valid @RequestBody AccountVerificationConfirmRequest request) {
        AccountVerificationConfirmResponse response =
                loanExecutionService.confirmAccountVerification(applicationId, request);
        return ApiResponse.onSuccess(LoanSuccessCode.ACCOUNT_VERIFICATION_CONFIRM_OK, response);
    }
}
