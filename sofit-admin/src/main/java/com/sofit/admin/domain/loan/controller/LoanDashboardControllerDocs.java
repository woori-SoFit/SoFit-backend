package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationInfoResponse;
import com.sofit.admin.domain.loan.dto.response.LoanDashboardResponse;
import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
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

    @Operation(
            summary = "대출 신청 상세 조회 (정보 탭)",
            description = "대출 신청 건의 정보 탭 데이터를 조회합니다. 신청자 정보, 사업자 정보, 대출 신청 정보, 고객 입력 정보, 약관 동의 이력을 포함합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "정보 탭 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대출 신청 건을 찾을 수 없음")
    })
    ApiResponse<LoanApplicationInfoResponse> findLoanApplicationInfo(
            @Parameter(description = "대출 신청 ID", example = "1") Long applicationId
    );

    @Operation(
            summary = "대출 신청 상세 조회 (My Biz Data 탭)",
            description = "대출 신청 건의 My Biz Data 탭 데이터를 조회합니다. 신청자의 연 소득, 보유 대출 건수, 월 매출액, 현금 흐름, 업종 순위 등 사업 현황 데이터를 포함합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "My Biz Data 탭 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대출 신청 건 또는 My Biz Data를 찾을 수 없음")
    })
    ApiResponse<MyBizDataDetailResponse> findMyBizDataDetail(
            @Parameter(description = "대출 신청 ID", example = "1") Long applicationId
    );
}
