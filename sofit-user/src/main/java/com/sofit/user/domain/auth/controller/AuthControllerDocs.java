package com.sofit.user.domain.auth.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertVerifyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;

@Tag(name = "인증", description = "회원가입 및 본인인증 API")
public interface AuthControllerDocs {

    @Operation(summary = "사업자등록번호 진위 확인", description = "KYC 인증 - 사업자등록번호 진위를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사업자 정보를 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<BusinessVerificationResponse>> verifyBusiness(
            BusinessVerificationRequest request,
            HttpSession session
    );

    @Operation(summary = "금융인증서 검증", description = "금융인증서 유효성 및 실명 일치 여부를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인증서를 찾을 수 없음")
    })
    ResponseEntity<ApiResponse<FinancialCertVerifyResponse>> verifyFinancialCertificate(
            FinancialCertVerifyRequest request
    );
}
