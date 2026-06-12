package com.sofit.admin.domain.dev.controller;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "개발자 - 대출 심사 배치 관리", description = "대출 심사 배치(loanDecisionJob) 실행 이력 조회 및 수동 실행 API")
public interface BatchTriggerControllerDocs {

    @Operation(summary = "대출 심사 배치 실행 이력 조회", description = "Spring Batch 메타데이터에서 loanDecisionJob 실행 이력을 페이징하여 조회합니다. 권한: ADMIN_DEV")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    })
    ApiResponse<BatchHistoryListResponse> findLoanDecisionBatchHistories(
            @Parameter(description = "페이지 번호 (0부터 시작, 기본값 0)") Integer page,
            @Parameter(description = "페이지당 건수 (기본값 5)") Integer size
    );

    @Operation(summary = "대출 심사 배치 수동 실행", description = "loanDecisionJob을 수동으로 트리거합니다. 권한: ADMIN_DEV")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "배치 실행 시작됨"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증이 필요합니다"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "배치 실행 실패")
    })
    ApiResponse<Void> triggerLoanDecisionBatch();
}
