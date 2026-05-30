package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.ManagerApprovalListResponse;

public interface ManagerApprovalService {

    ManagerApprovalListResponse findManagerReviewApplications();
}
