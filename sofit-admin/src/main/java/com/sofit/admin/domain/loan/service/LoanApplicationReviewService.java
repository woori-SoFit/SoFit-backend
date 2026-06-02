package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationReviewResponse;

public interface LoanApplicationReviewService {

    LoanApplicationReviewResponse findLoanApplicationReview(Long applicationId);
}
