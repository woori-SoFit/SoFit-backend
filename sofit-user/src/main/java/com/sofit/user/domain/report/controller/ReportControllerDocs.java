package com.sofit.user.domain.report.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.report.dto.response.GradeDetailResponse;
import com.sofit.user.domain.report.dto.response.GradeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "성장 S등급 리포트", description = "성장 S등급 분석 리포트 관련 API")
public interface ReportControllerDocs {

    @Operation(summary = "성장 S등급 결과 조회", description = "로그인한 사용자의 최신 성장 S등급 결과를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "성장 S등급 미산출")
    })
    ApiResponse<GradeResponse> findGrade();

    @Operation(summary = "성장 S등급 상세 리포트 조회", description = "로그인한 사용자의 성장 S등급 상세 리포트를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "성장 S등급 미산출")
    })
    ApiResponse<GradeDetailResponse> findGradeDetail();
}
