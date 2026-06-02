package com.sofit.admin.domain.auth.controller;

import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
import com.sofit.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Tag(name = "관리자 인증")
public interface AdminAuthControllerDocs {

    @Operation(
            summary = "관리자 페이지 로그인",
            description = "관리자(은행원/지점장/개발자) 계정으로 로그인합니다. 일반 고객(USER)은 접근할 수 없습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "아이디 또는 비밀번호 불일치")
    })
    ApiResponse<AdminLoginResponse> login(AdminLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    @Operation(
            summary = "관리자 내 정보 조회",
            description = "로그인한 관리자의 프로필 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "세션 만료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 미존재")
    })
    ApiResponse<AdminMeResponse> findMe();

    @Operation(
            summary = "관리자 페이지 로그아웃",
            description = "현재 로그인된 관리자의 세션을 삭제하고 로그아웃합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    ApiResponse<Void> logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
}
