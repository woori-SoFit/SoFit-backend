package com.sofit.admin.domain.dev.controller;

import com.sofit.admin.domain.dev.dto.response.UserListResponse;
import com.sofit.admin.domain.dev.dto.response.UserStatisticsResponse;
import com.sofit.admin.domain.dev.service.DevUserService;
import com.sofit.admin.global.util.AdminRoleService;
import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.apiPayload.code.GeneralSuccessCode;
import com.sofit.common.entity.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class DevUserController implements DevUserControllerDocs {

    private final DevUserService devUserService;
    private final AdminRoleService adminRoleService;

    @GetMapping
    @Override
    public ApiResponse<UserListResponse> findUsers(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        UserListResponse response = devUserService.findUsers(page, size, keyword, role, status);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    @GetMapping("/statistics")
    @Override
    public ApiResponse<UserStatisticsResponse> findUserStatistics() {
        UserRole role = adminRoleService.getCurrentUserRole();

        if (role != UserRole.ADMIN_DEV
                && role != UserRole.ADMIN_BANK_TELLER
                && role != UserRole.ADMIN_BANK_MANAGER) {
            throw new BaseException(GeneralErrorCode.FORBIDDEN);
        }

        UserStatisticsResponse response = devUserService.findUserStatistics();
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
