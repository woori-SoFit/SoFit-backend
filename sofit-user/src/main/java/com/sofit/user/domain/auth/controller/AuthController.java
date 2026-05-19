package com.sofit.user.domain.auth.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.request.LoginRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertVerifyResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.exception.AuthSuccessCode;
import com.sofit.user.domain.auth.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

    private final AuthService authService;

    @PostMapping("/signup/business-verification")
    public ResponseEntity<ApiResponse<BusinessVerificationResponse>> verifyBusiness(
            @Valid @RequestBody BusinessVerificationRequest request) {

        BusinessVerificationResponse response = authService.verifyBusiness(request);

        return ResponseEntity.ok(
                ApiResponse.onSuccess(AuthSuccessCode.BUSINESS_VERIFIED, response)
        );
    }

    @PostMapping("/financial-certificate/verify")
    public ResponseEntity<ApiResponse<FinancialCertVerifyResponse>> verifyFinancialCertificate(
            @Valid @RequestBody FinancialCertVerifyRequest request) {

        FinancialCertVerifyResponse response = authService.verifyFinancialCertificate(request);

        return ResponseEntity.ok(
                ApiResponse.onSuccess(AuthSuccessCode.PIN_VERIFIED, response)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpSession session) {

        LoginResponse response = authService.login(request, session);

        return ResponseEntity.ok(
                ApiResponse.onSuccess(AuthSuccessCode.LOGIN_SUCCESS, response)
        );
    }
}
