package com.sofit.admin.domain.dev.controller;

import com.sofit.admin.domain.dev.dto.response.UserListResponse;
import com.sofit.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "개발자 - 사용자 관리", description = "고객 정보 목록 조회 API")
public interface DevUserControllerDocs {

    @Operation(summary = "고객 정보 목록 조회", description = "페이징 및 필터 조건으로 사용자 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    ApiResponse<UserListResponse> findUsers(
            @Parameter(description = "페이지 번호 (1부터 시작)", required = true) Integer page,
            @Parameter(description = "페이지당 건수 (기본 8)") Integer size,
            @Parameter(description = "검색어 (이름, 아이디 부분 매칭)") String keyword,
            @Parameter(description = "역할 필터 (ADMIN_DEV, ADMIN_BANK_MANAGER, ADMIN_BANK_TELLER, USER)") String role,
            @Parameter(description = "상태 필터 (ACTIVE, INACTIVE)") String status
    );
}
