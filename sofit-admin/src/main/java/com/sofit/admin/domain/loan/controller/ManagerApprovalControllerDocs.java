package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.response.ManagerApprovalListResponse;
import com.sofit.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "지점장 결재")
public interface ManagerApprovalControllerDocs {

    @Operation(summary = "지점장 결재 대기 목록 조회")
    ApiResponse<ManagerApprovalListResponse> findManagerApprovalList();
}
