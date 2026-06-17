package com.sofit.user.domain.mybiz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.exception.MyBizSuccessCode;
import com.sofit.user.domain.mybiz.service.MyBizService;
import com.sofit.user.global.util.SecurityUtil;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/mybiz")
@RequiredArgsConstructor
public class MyBizController implements MyBizControllerDocs {

    private final MyBizService myBizService;

    @GetMapping("/dashboard")
    public ApiResponse<MyBizDashboardResponse> findDashboard(
            @RequestParam(value = "month", required = false) String month) {
        Long userId = SecurityUtil.getCurrentUserId();
        MyBizDashboardResponse response = myBizService.findDashboard(userId, month);
        return ApiResponse.onSuccess(MyBizSuccessCode.DASHBOARD_OK, response);
    }
}
