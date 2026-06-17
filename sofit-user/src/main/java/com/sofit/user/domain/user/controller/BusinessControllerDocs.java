package com.sofit.user.domain.user.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "사업자", description = "사업자 정보 관련 API")
public interface BusinessControllerDocs {

    @Operation(summary = "내 사업자 정보 조회", description = "로그인한 사용자의 사업자 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사업자 정보 미존재")
    })
    ApiResponse<BusinessProfileResponse> findBusinessProfile();

    @Operation(summary = "마이비즈 데이터 연동 완료", description = "마이비즈 데이터 수집 완료 후 연동 상태를 true로 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "연동 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사업자 정보 미존재")
    })
    ApiResponse<Void> connectMybiz();
}
