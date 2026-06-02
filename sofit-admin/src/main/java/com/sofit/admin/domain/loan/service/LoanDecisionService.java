package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.request.LoanApproveRequest;
import com.sofit.admin.domain.loan.dto.request.LoanRejectRequest;
import com.sofit.admin.domain.loan.dto.response.LoanDecisionResponse;

public interface LoanDecisionService {

    LoanDecisionResponse approveLoanApplication(Long applicationId, LoanApproveRequest request);

    LoanDecisionResponse rejectLoanApplication(Long applicationId, LoanRejectRequest request);
}
