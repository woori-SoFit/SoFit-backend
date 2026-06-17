package com.sofit.admin.domain.dev.controller;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.admin.domain.dev.dto.response.BatchStatusResponse;
import com.sofit.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "개발자 - 배치 관리", description = "성장 S등급 배치 실행 이력 조회 및 수동 실행 API")
public interface DevBatchControllerDocs {

    @Operation(summary = "성장 S등급 배치 실행 이력 조회", description = "배치 실행 이력을 페이징하여 조회합니다. 권한: ADMIN_DEV")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    ApiResponse<BatchHistoryListResponse> findBatchHistories(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") Integer page,
            @Parameter(description = "페이지당 건수 (기본값 5)") Integer size
    );

    @Operation(summary = "수동 S등급 배치 실행", description = "AI 서버에 S등급 배치 실행을 수동 트리거합니다. 세션에서 관리자 userId를 자동 추출하여 전달합니다. 이미 실행 중이면 409를 반환합니다. 권한: ADMIN_DEV")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "배치 실행 시작됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 배치 실행 중"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 서버 오류 (내부 에러 또는 비정상 응답)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 서버 연결 실패 (타임아웃)")
    })
    ApiResponse<Void> triggerSGradeBatch();

    @Operation(summary = "S등급 배치 실행 상태 조회", description = "AI 서버의 배치 실행 상태를 조회합니다. 권한: ADMIN_DEV")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상태 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 서버 오류 (내부 에러 또는 비정상 응답)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "AI 서버 연결 실패 (타임아웃)")
    })
    ApiResponse<BatchStatusResponse> getSGradeBatchStatus();
}
