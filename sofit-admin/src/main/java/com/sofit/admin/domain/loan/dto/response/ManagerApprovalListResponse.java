package com.sofit.admin.domain.loan.dto.response;

import java.util.List;

public record ManagerApprovalListResponse(
        List<ManagerApprovalItemResponse> applications
) {
}
