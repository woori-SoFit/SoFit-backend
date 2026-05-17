package com.sofit.user.domain.loan.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.exception.LoanSuccessCode;
import com.sofit.user.domain.loan.service.LoanService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanService loanService;

    // TODO: 세션 인증 구현 후 SecurityContext에서 userId 추출하도록 변경
    private static final Long TEMP_USER_ID = 1L;

    /**
     * 심사 중인 대출 목록 조회
     * GET /api/loan-applications
     */
    @GetMapping
    public ApiResponse<LoanApplicationListResponse> getUnderReviewLoans() {
        LoanApplicationListResponse response = loanService.findUnderReviewLoans(TEMP_USER_ID);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_LIST_OK, response);
    }

    /**
     * 심사 중인 대출 상세 조회
     * GET /api/loan-applications/{applicationId}
     */
    @GetMapping("/{applicationId}")
    public ApiResponse<LoanApplicationDetailResponse> getLoanDetail(
            @PathVariable Long applicationId) {
        LoanApplicationDetailResponse response = loanService.findLoanDetail(TEMP_USER_ID, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_DETAIL_OK, response);
    }

    /**
     * 심사 완료 대출 목록 조회
     * GET /api/loan-applications/completed
     */
    @GetMapping("/completed")
    public ApiResponse<CompletedLoanListResponse> getCompletedLoans() {
        CompletedLoanListResponse response = loanService.findCompletedLoans(TEMP_USER_ID);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_COMPLETED_LIST_OK, response);
    }

    /**
     * 심사 완료 대출 상세 조회
     * GET /api/loan-applications/completed/{applicationId}
     */
    @GetMapping("/completed/{applicationId}")
    public ApiResponse<CompletedLoanDetailResponse> getCompletedLoanDetail(
            @PathVariable Long applicationId) {
        CompletedLoanDetailResponse response = loanService.findCompletedLoanDetail(TEMP_USER_ID, applicationId);
        return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_COMPLETED_DETAIL_OK, response);
    }
}
