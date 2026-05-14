package com.sofit.user.domain.auth.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.exception.AuthSuccessCode;
import com.sofit.user.domain.auth.service.AuthService;
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
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup/business-verification")
    public ResponseEntity<ApiResponse<BusinessVerificationResponse>> verifyBusiness(
            @Valid @RequestBody BusinessVerificationRequest request) {

        // TODO: 임시 세션에서 userId 가져오기 (회원가입 플로우 구현 시 변경)
        Long userId = 1L;

        BusinessVerificationResponse response = authService.verifyBusiness(request, userId);

        return ResponseEntity.ok(
                ApiResponse.onSuccess(AuthSuccessCode.BUSINESS_VERIFIED, response)
        );
    }
}
