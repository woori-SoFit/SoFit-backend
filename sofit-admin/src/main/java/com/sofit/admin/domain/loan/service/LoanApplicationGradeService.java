package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationGradeResponse;

public interface LoanApplicationGradeService {

    LoanApplicationGradeResponse findLoanApplicationGrade(Long applicationId);
}
