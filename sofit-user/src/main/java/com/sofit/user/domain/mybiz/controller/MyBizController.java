package com.sofit.user.domain.mybiz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.exception.MyBizSuccessCode;
import com.sofit.user.domain.mybiz.service.MyBizService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/my-biz")
@RequiredArgsConstructor
public class MyBizController implements MyBizControllerDocs {

    private final MyBizService myBizService;

    @GetMapping("/dashboard")
    public ApiResponse<MyBizDashboardResponse> findDashboard(
            HttpServletRequest request,
            @RequestParam(value = "month", required = false) String month) {
        Long userId = extractUserId(request);
        MyBizDashboardResponse response = myBizService.findDashboard(userId, month);
        return ApiResponse.onSuccess(MyBizSuccessCode.DASHBOARD_OK, response);
    }

    /**
     * 세션에서 userId를 추출한다.
     * 세션이 없거나 userId가 없으면 UNAUTHORIZED 예외 발생
     */
    private Long extractUserId(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new BaseException(GeneralErrorCode.UNAUTHORIZED);
        }
        Object userIdAttr = session.getAttribute("userId");
        return (userIdAttr instanceof Long) ? (Long) userIdAttr : Long.valueOf(userIdAttr.toString());
    }
}
