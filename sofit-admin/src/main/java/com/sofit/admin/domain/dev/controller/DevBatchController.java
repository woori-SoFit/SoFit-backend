package com.sofit.admin.domain.dev.controller;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.admin.domain.dev.service.DevBatchService;
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
@RequestMapping("/api/admin/dev/batch")
@RequiredArgsConstructor
public class DevBatchController implements DevBatchControllerDocs {

    private final DevBatchService devBatchService;
    private final AdminRoleService adminRoleService;

    @GetMapping("/s-grade")
    @Override
    public ApiResponse<BatchHistoryListResponse> findBatchHistories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        UserRole role = adminRoleService.getCurrentUserRole();

        if (role != UserRole.ADMIN_DEV) {
            throw new BaseException(GeneralErrorCode.FORBIDDEN);
        }

        BatchHistoryListResponse response = devBatchService.findBatchHistories(page, size);
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }
}
