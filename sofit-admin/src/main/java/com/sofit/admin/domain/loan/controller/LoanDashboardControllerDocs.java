package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.admin.domain.loan.dto.response.LoanDashboardResponse;
import com.sofit.common.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "대출 대시보드")
public interface LoanDashboardControllerDocs {

    @Operation(
            summary = "대출 신청 목록 조회",
            description = "심사 단계에 진입한 대출 신청 건을 페이징 조회합니다. 상태 필터(다중 선택 가능)와 담당 은행원 ID 필터를 지원합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대출 신청 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 심사 상태")
    })
    ApiResponse<LoanDashboardResponse> findLoanApplications(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") Integer page,
            @Parameter(description = "페이지 크기", example = "10") Integer size,
            @Parameter(description = "심사 상태 필터 (SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED). 다중 선택 가능") List<String> status,
            @Parameter(description = "담당 은행원 ID 필터") Long assignedBankerId
    );

    @Operation(
            summary = "대출 신청 상세 조회 (공통 정보)",
            description = "대출 신청 건의 공통 정보를 단건 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대출 신청 상세 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대출 신청 건을 찾을 수 없음")
    })
    ApiResponse<LoanApplicationDetailResponse> findLoanApplicationDetail(
            @Parameter(description = "대출 신청 ID", example = "1") Long applicationId
    );
}
