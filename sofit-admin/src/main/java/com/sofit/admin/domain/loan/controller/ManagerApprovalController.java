package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.response.ManagerApprovalListResponse;
import com.sofit.admin.domain.loan.exception.ManagerApprovalSuccessCode;
import com.sofit.admin.domain.loan.service.ManagerApprovalService;
import com.sofit.admin.global.util.AdminRoleService;
import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/manager")
@RequiredArgsConstructor
public class ManagerApprovalController implements ManagerApprovalControllerDocs {

    private final AdminRoleService adminRoleService;
    private final ManagerApprovalService managerApprovalService;

    @GetMapping("/loan-applications")
    @Override
    public ApiResponse<ManagerApprovalListResponse> findManagerApprovalList() {
        UserRole role = adminRoleService.getCurrentUserRole();

        if (role != UserRole.ADMIN_BANK_MANAGER && role != UserRole.ADMIN_DEV) {
            throw new BaseException(GeneralErrorCode.FORBIDDEN);
        }

        ManagerApprovalListResponse response = managerApprovalService.findManagerReviewApplications();
        return ApiResponse.onSuccess(ManagerApprovalSuccessCode.MANAGER_APPROVAL_LIST_OK, response);
    }
}
