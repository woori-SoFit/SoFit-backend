package com.sofit.user.domain.auth.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertVerifyResponse;
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
            @Valid @RequestBody BusinessVerificationRequest request,
            HttpSession session) {

        BusinessVerificationResponse response = authService.verifyBusiness(request);

        // 인증 결과를 세션에 임시 저장 (회원가입 완료 시 DB 저장에 사용)
        session.setAttribute("kycVerified", true);
        session.setAttribute("kycResult", response);

        return ResponseEntity.ok(
                ApiResponse.onSuccess(AuthSuccessCode.BUSINESS_VERIFIED, response)
        );
    }

    @PostMapping("/financial-certificate/verify")
    public ResponseEntity<ApiResponse<FinancialCertVerifyResponse>> verifyFinancialCertificate(
            @Valid @RequestBody FinancialCertVerifyRequest request) {

        FinancialCertVerifyResponse response = authService.verifyFinancialCertificate(request);

        return ResponseEntity.ok(
                ApiResponse.onSuccess(AuthSuccessCode.FINANCIAL_CERT_VERIFIED, response)
        );
    }
}
