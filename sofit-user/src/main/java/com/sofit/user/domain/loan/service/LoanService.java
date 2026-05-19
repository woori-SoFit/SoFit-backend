package com.sofit.user.domain.loan.service;

import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;

public interface LoanService {

    // 본인의 심사 중인 대출 목록 조회 (SUBMITTED ~ FINAL_REVIEW)
    LoanApplicationListResponse findUnderReviewLoans(Long userId);

    // 본인의 심사 중인 대출 상세 조회
    LoanApplicationDetailResponse findLoanDetail(Long userId, Long applicationId);

    // 본인의 심사 완료 대출 목록 조회 (APPROVED, REJECTED)
    CompletedLoanListResponse findCompletedLoans(Long userId);

    // 본인의 심사 완료 대출 상세 조회
    CompletedLoanDetailResponse findCompletedLoanDetail(Long userId, Long applicationId);
}
