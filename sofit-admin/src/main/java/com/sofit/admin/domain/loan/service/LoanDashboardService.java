package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.LoanDashboardResponse;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LoanDashboardService {

    LoanDashboardResponse findLoanApplications(List<ApplicationStatus> statuses, Long assignedBankerId, Pageable pageable);
}
