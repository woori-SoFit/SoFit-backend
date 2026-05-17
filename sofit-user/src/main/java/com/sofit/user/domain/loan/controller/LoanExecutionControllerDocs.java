package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "대출 실행", description = "대출 실행 결과 조회 API")
public interface LoanExecutionControllerDocs {

    @Operation(summary = "대출 실행 결과 조회", description = "EXECUTED 상태인 대출 건의 실행 결과를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "실행 건을 찾을 수 없습니다.")
    })
    ApiResponse<LoanExecutionResultResponse> getExecutionResult(
            @Parameter(description = "대출 신청 ID", required = true, example = "10") Long applicationId
    );
}
