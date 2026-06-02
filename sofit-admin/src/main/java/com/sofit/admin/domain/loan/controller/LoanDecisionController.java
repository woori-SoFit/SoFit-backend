package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.request.LoanApproveRequest;
import com.sofit.admin.domain.loan.dto.request.LoanRejectRequest;
import com.sofit.admin.domain.loan.dto.response.LoanDecisionResponse;
import com.sofit.admin.domain.loan.exception.LoanDecisionSuccessCode;
import com.sofit.admin.domain.loan.service.LoanDecisionService;
import com.sofit.common.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/loan-applications")
@RequiredArgsConstructor
public class LoanDecisionController implements LoanDecisionControllerDocs {

    private final LoanDecisionService loanDecisionService;

    @PostMapping("/{applicationId}/approve")
    @Override
    public ApiResponse<LoanDecisionResponse> approveLoanApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody LoanApproveRequest request) {
        LoanDecisionResponse response = loanDecisionService.approveLoanApplication(applicationId, request);
        return ApiResponse.onSuccess(LoanDecisionSuccessCode.LOAN_APPROVE_OK, response);
    }

    @PostMapping("/{applicationId}/reject")
    @Override
    public ApiResponse<LoanDecisionResponse> rejectLoanApplication(
            @PathVariable Long applicationId,
            @Valid @RequestBody LoanRejectRequest request) {
        LoanDecisionResponse response = loanDecisionService.rejectLoanApplication(applicationId, request);
        return ApiResponse.onSuccess(LoanDecisionSuccessCode.LOAN_REJECT_OK, response);
    }
}
