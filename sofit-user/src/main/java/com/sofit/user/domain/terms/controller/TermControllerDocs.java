package com.sofit.user.domain.terms.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.user.domain.terms.dto.response.TermListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "약관", description = "약관 조회 API")
public interface TermControllerDocs {

    @Operation(summary = "약관 목록 조회", description = "약관 타입별 현행 약관 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 termType")
    })
    ApiResponse<TermListResponse> getTerms(
            @Parameter(description = "약관 타입", required = true, example = "LOAN_APPLICATION") TermType termType
    );
}
