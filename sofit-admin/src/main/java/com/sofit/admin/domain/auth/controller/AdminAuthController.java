package com.sofit.admin.domain.auth.controller;

import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
import com.sofit.admin.domain.auth.exception.AdminAuthSuccessCode;
import com.sofit.admin.domain.auth.service.AdminAuthService;
import com.sofit.common.apiPayload.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController implements AdminAuthControllerDocs {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    @Override
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request,
                                                  HttpServletRequest httpRequest,
                                                  HttpServletResponse httpResponse) {
        AdminLoginResponse response = adminAuthService.login(request, httpRequest, httpResponse);
        return ApiResponse.onSuccess(AdminAuthSuccessCode.LOGIN_SUCCESS, response);
    }

    @GetMapping("/me")
    @Override
    public ApiResponse<AdminMeResponse> findMe() {
        AdminMeResponse response = adminAuthService.findMe();
        return ApiResponse.onSuccess(AdminAuthSuccessCode.ME_SUCCESS, response);
    }

    @PostMapping("/logout")
    @Override
    public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        adminAuthService.logout(httpRequest, httpResponse);
        return ApiResponse.onSuccess(AdminAuthSuccessCode.LOGOUT_SUCCESS, null);
    }
}
