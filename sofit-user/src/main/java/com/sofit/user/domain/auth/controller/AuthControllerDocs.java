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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃 API")
public interface AuthControllerDocs {

    @Operation(summary = "사업자등록번호 진위 확인", description = "KYC 인증 - 사업자등록번호 진위를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 가입된 사업자"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사업자 정보를 찾을 수 없음")
    })
    ApiResponse<BusinessVerificationResponse> verifyBusiness(
            BusinessVerificationRequest request,
            HttpSession session
    );

    @Operation(summary = "금융인증서 본인인증 + PIN 검증", description = "금융인증서 본인인증과 PIN 검증을 수행합니다. 회원가입/대출 등 세션 컨텍스트에 따라 후처리가 분기됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "본인인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "PIN 불일치, 단계 미완료, 만료, 인증서 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "인증서를 찾을 수 없음")
    })
    ApiResponse<Void> verifyFinancialCertificate(
            FinancialCertVerifyRequest request,
            HttpSession session
    );

    @Operation(summary = "회원가입 완료", description = "회원가입 Step 3 - 고객 정보를 입력받아 회원가입을 완료합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "단계 미완료, 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "아이디 중복")
    })
    ApiResponse<SignupCompleteResponse> completeSignup(
            SignupCompleteRequest request,
            HttpSession session
    );

    @Operation(summary = "로그인", description = "아이디와 비밀번호로 로그인합니다. 세션 쿠키가 발급됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "탈퇴한 계정")
    })
    ApiResponse<LoginResponse> login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    );

    @Operation(summary = "로그인 아이디 중복 확인", description = "회원가입 시 로그인 아이디의 사용 가능 여부를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확인 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "아이디 형식 오류 (영문/숫자 4~20자)")
    })
    ApiResponse<CheckLoginIdResponse> checkLoginId(
            @Parameter(description = "중복 확인할 로그인 아이디 (영문/숫자 4~20자)", required = true, example = "testuser1")
            String loginId
    );

    @Operation(summary = "로그아웃", description = "현재 세션을 무효화하고 로그아웃합니다. JSESSIONID 쿠키도 만료 처리됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 요청")
    })
    ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response);
}
