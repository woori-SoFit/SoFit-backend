package com.sofit.admin.domain.auth.controller;

import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;

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
    ApiResponse<AdminLoginResponse> login(AdminLoginRequest request, HttpSession session);
}
