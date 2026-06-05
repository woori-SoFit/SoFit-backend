package com.sofit.user.domain.auth.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.request.LoginRequest;
import com.sofit.user.domain.auth.dto.request.SignupCompleteRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.CheckLoginIdResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.dto.response.SignupCompleteResponse;
import com.sofit.user.domain.auth.exception.AuthSuccessCode;
import com.sofit.user.domain.auth.service.AuthService;
import com.sofit.user.global.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @PostMapping("/signup/business-verification")
    public ApiResponse<BusinessVerificationResponse> verifyBusiness(
            @Valid @RequestBody BusinessVerificationRequest request,
            HttpSession session) {

        BusinessVerificationResponse response = authService.verifyBusiness(request, session);
        return ApiResponse.onSuccess(AuthSuccessCode.BUSINESS_VERIFIED, response);
    }

    @PostMapping("/signup/verify-pin")
    public ApiResponse<Void> verifyFinancialCertificate(
            @Valid @RequestBody FinancialCertVerifyRequest request,
            HttpSession session) {

        authService.verifyFinancialCertificate(request, session);
        return ApiResponse.onSuccess(AuthSuccessCode.PIN_VERIFIED, null);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup/complete")
    public ApiResponse<SignupCompleteResponse> completeSignup(
            @Valid @RequestBody SignupCompleteRequest request,
            HttpSession session) {

        SignupCompleteResponse response = authService.completeSignup(request, session);
        return ApiResponse.onSuccess(AuthSuccessCode.SIGNUP_COMPLETED, response);
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        LoginResponse response = authService.login(request, httpRequest, httpResponse);
        return ApiResponse.onSuccess(AuthSuccessCode.LOGIN_SUCCESS, response);
    }

    @GetMapping("/signup/check-login-id")
    public ApiResponse<CheckLoginIdResponse> checkLoginId(
            @RequestParam String loginId) {

        CheckLoginIdResponse response = authService.checkLoginId(loginId);
        return ApiResponse.onSuccess(AuthSuccessCode.LOGIN_ID_CHECKED, response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SessionUtil.invalidateSession(httpRequest, httpResponse);

        return ApiResponse.onSuccess(AuthSuccessCode.LOGOUT_SUCCESS, null);
    }
}
