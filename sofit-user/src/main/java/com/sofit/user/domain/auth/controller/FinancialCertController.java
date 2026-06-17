package com.sofit.user.domain.auth.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.auth.dto.request.FinancialCertLookupRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.FinancialCertLookupResponse;
import com.sofit.user.domain.auth.exception.AuthSuccessCode;
import com.sofit.user.domain.auth.service.FinancialCertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 범용 금융인증서 조회 및 본인인증 컨트롤러.
 * 대출 플로우, 마이비즈 플로우 등에서 공통으로 사용.
 * 로그인 필요 (anyRequest().authenticated()에 의해 보호됨).
 */
@RestController
@RequestMapping("/api/financial-cert")
@RequiredArgsConstructor
public class FinancialCertController implements FinancialCertControllerDocs {

    private final FinancialCertService financialCertService;

    @PostMapping("/lookup")
    @Override
    public ApiResponse<FinancialCertLookupResponse> lookup(
            @Valid @RequestBody FinancialCertLookupRequest request) {
        FinancialCertLookupResponse response = financialCertService.lookup(request);
        return ApiResponse.onSuccess(AuthSuccessCode.CERT_LOOKUP_SUCCESS, response);
    }

    @PostMapping("/verify-pin")
    @Override
    public ApiResponse<Void> verifyPin(
            @Valid @RequestBody FinancialCertVerifyRequest request) {
        financialCertService.verify(request);
        return ApiResponse.onSuccess(AuthSuccessCode.PIN_VERIFIED, null);
    }
}
