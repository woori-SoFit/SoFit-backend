package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.request.LoanApproveRequest;
import com.sofit.admin.domain.loan.dto.request.LoanRejectRequest;
import com.sofit.admin.domain.loan.dto.response.LoanDecisionResponse;
import com.sofit.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "대출 심사 결정", description = "대출 신청 건에 대한 승인/거절 처리 API")
public interface LoanDecisionControllerDocs {

    @Operation(
            summary = "대출 승인 처리",
            description = "대출 신청 건을 승인합니다. 승인 금액, 확정 금리, 확정 기간, 상환 방식, comment를 입력합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대출 승인 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (필수값 누락, 유효하지 않은 값)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대출 신청 건을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 승인/거절 처리된 신청 건")
    })
    ApiResponse<LoanDecisionResponse> approveLoanApplication(
            @Parameter(description = "대출 신청 ID", example = "10018") Long applicationId,
            LoanApproveRequest request
    );

    @Operation(
            summary = "대출 거절 처리",
            description = "대출 신청 건을 거절합니다. comment(거절 사유)를 입력합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대출 거절 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (comment 누락)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대출 신청 건을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 승인/거절 처리된 신청 건")
    })
    ApiResponse<LoanDecisionResponse> rejectLoanApplication(
            @Parameter(description = "대출 신청 ID", example = "10018") Long applicationId,
            LoanRejectRequest request
    );
}
