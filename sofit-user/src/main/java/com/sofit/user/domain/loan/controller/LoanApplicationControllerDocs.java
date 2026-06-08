package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.request.LoanApplicationCreateRequest;
import com.sofit.user.domain.loan.dto.request.LoanApplicationSubmitRequest;
import com.sofit.user.domain.loan.dto.response.CompletedLoanDetailResponse;
import com.sofit.user.domain.loan.dto.response.CompletedLoanListResponse;
import com.sofit.user.domain.loan.dto.response.DraftCheckResponse;
import com.sofit.user.domain.loan.dto.response.DraftListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationCreateResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationListResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationResumeResponse;
import com.sofit.user.domain.loan.dto.response.LoanApplicationSubmitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "대출 신청", description = "대출 신청 및 심사 현황 조회 API")
public interface LoanApplicationControllerDocs {

    @Operation(summary = "대출 신청 시작", description = "1차 필터링 통과 후 DRAFT 상태의 대출 신청을 시작합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신청 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "상품이 비활성 상태"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "KYC 미완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "상품을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "동일 상품 중복 신청")
    })
    ApiResponse<LoanApplicationCreateResponse> createApplication(
            @Parameter(description = "대출 상품 ID", required = true, example = "1") Long productId,
            LoanApplicationCreateRequest request);

    @Operation(summary = "DRAFT 존재 여부 확인", description = "특정 상품에 대해 현재 사용자의 DRAFT 상태 신청이 있는지 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<DraftCheckResponse> checkDraft(
            @Parameter(description = "대출 상품 ID", required = true, example = "1") Long productId);

    @Operation(summary = "진행 중인 DRAFT 목록 조회", description = "현재 사용자의 모든 DRAFT 상태 대출 신청 목록을 상품명 포함하여 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<DraftListResponse> getDrafts();

    @Operation(summary = "이어가기 데이터 조회", description = "DRAFT 상태인 대출 신청의 저장된 데이터를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 건을 찾을 수 없음")
    })
    ApiResponse<LoanApplicationResumeResponse> getResumeData(
            @Parameter(description = "대출 신청 ID", required = true, example = "1") Long applicationId);

    @Operation(summary = "DRAFT 신청서 취소", description = "DRAFT 상태의 대출 신청서를 취소(소프트 삭제)합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "DRAFT 상태가 아닌 신청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 신청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 신청")
    })
    ApiResponse<Void> cancelDraftApplication(
            @Parameter(description = "대출 신청 ID", required = true, example = "1") Long applicationId);    
   
    @Operation(summary = "최종 제출 (심사 요청)", description = "DRAFT 상태의 대출 신청을 최종 제출하여 심사를 요청합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "심사 요청 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "DRAFT 상태가 아닌 신청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 건을 찾을 수 없음")
    })
    ApiResponse<LoanApplicationSubmitResponse> submitApplication(
            @Parameter(description = "대출 신청 ID", required = true, example = "1") Long applicationId,
            LoanApplicationSubmitRequest request
    );

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
