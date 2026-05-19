package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "대출 신청", description = "대출 신청 및 심사 현황 조회 API")
public interface LoanApplicationControllerDocs {

    @Operation(summary = "심사 중인 대출 목록 조회", description = "현재 사용자의 심사 중인 대출 신청 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<LoanApplicationListResponse> getUnderReviewLoans();

    @Operation(summary = "심사 중인 대출 상세 조회", description = "대출 신청 ID로 심사 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 건을 찾을 수 없음")
    })
    ApiResponse<LoanApplicationDetailResponse> getLoanDetail(
            @Parameter(description = "대출 신청 ID", required = true, example = "1") Long applicationId
    );

    @Operation(summary = "심사 완료 대출 목록 조회",
            description = "현재 사용자의 심사 완료(승인/거절) 대출 신청 목록을 updatedAt 내림차순으로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<CompletedLoanListResponse> getCompletedLoans();

    @Operation(summary = "심사 완료 대출 상세 조회",
            description = "심사 완료된 대출 신청 ID로 결정 정보(승인 금액/금리/기간 또는 거절 사유)를 포함한 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 건 또는 심사 결정을 찾을 수 없음")
    })
    ApiResponse<CompletedLoanDetailResponse> getCompletedLoanDetail(
            @Parameter(description = "대출 신청 ID", required = true, example = "5") Long applicationId
    );
}
