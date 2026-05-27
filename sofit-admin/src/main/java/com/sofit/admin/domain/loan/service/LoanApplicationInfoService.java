package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationInfoResponse;

public interface LoanApplicationInfoService {

    LoanApplicationInfoResponse findLoanApplicationInfo(Long applicationId);
}
