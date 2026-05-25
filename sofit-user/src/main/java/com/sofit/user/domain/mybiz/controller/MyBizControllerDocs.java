package com.sofit.user.domain.mybiz.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "My Biz Data", description = "소상공인 My Biz Data 대시보드 조회 API")
public interface MyBizControllerDocs {

    @Operation(summary = "My Biz 대시보드 조회",
               description = "로그인한 사용자의 My Biz Data 대시보드를 조회합니다. month 파라미터로 특정 기준월을 지정할 수 있습니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "month 형식 오류 (yyyy-MM 형식이 아닌 경우)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "My Biz Data 미존재")
    })
    ApiResponse<MyBizDashboardResponse> findDashboard(
            @Parameter(description = "조회 기준월 (yyyy-MM 형식)", required = false, example = "2024-05")
            String month);
}
