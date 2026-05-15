package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.exception.LoanSuccessCode;
import com.sofit.user.domain.loan.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    public ApiResponse<List<LoanApplicationListResponse>> getUnderReviewLoans() {
        return ApiResponse.onSuccess(
                LoanSuccessCode.LOAN_APPLICATION_LIST_OK,
                loanService.findUnderReviewLoans(TEMP_USER_ID)
        );
    }

    /**
     * 심사 중인 대출 상세 조회
     * GET /api/loan-applications/{applicationId}
     */
    @GetMapping("/{applicationId}")
    public ApiResponse<LoanApplicationDetailResponse> getLoanDetail(
            @PathVariable Long applicationId) {
        return ApiResponse.onSuccess(
                LoanSuccessCode.LOAN_APPLICATION_DETAIL_OK,
                loanService.findLoanDetail(TEMP_USER_ID, applicationId)
        );
    }
}
