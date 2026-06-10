package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.loan.dto.request.AccountVerificationConfirmRequest;
import com.sofit.user.domain.loan.dto.request.AccountVerificationRequest;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionListResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "대출 실행", description = "대출 실행 결과 조회 및 계좌 인증 API")
public interface LoanExecutionControllerDocs {

    @Operation(summary = "대출 실행 완료 목록 조회", description = "현재 로그인한 사용자의 EXECUTED 상태인 대출 실행 완료 건을 목록으로 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 (LOAN2019)")
    })
    ApiResponse<LoanExecutionListResponse> getExecutionList();

    @Operation(summary = "대출 실행 결과 조회", description = "EXECUTED 상태인 대출 건의 실행 결과를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공 (LOAN2010)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "실행 건을 찾을 수 없거나(LOAN4044) 심사 결정 정보가 없음(LOAN4043)")
    })
    ApiResponse<LoanExecutionResultResponse> getExecutionResult(
            @Parameter(description = "대출 신청 ID", required = true, example = "10") Long applicationId
    );

    @Operation(summary = "1원 송금 요청", description = "계좌 인증을 위해 1원 송금을 요청합니다. 코데프 API를 통해 해당 계좌로 1원이 송금되며, 입금자명에 4자리 인증코드가 포함됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "1원 송금 요청 성공 (LOAN2011)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 계좌번호(ACCOUNT4001) 또는 은행코드(ACCOUNT4005)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "일일 요청 한도 초과(ACCOUNT4002)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "계좌 인증 서비스 오류(ACCOUNT5001)")
    })
    ApiResponse<AccountVerificationResponse> requestAccountVerification(
            @Parameter(description = "대출 신청 ID", required = true, example = "10") Long applicationId,
            AccountVerificationRequest request
    );

    @Operation(summary = "인증코드 확인", description = "통장에서 확인한 4자리 인증코드를 입력하여 계좌 인증을 완료합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "계좌 인증 성공 (LOAN2012)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증번호 불일치(ACCOUNT4003) 또는 인증 시간 만료(ACCOUNT4004)")
    })
    ApiResponse<AccountVerificationConfirmResponse> confirmAccountVerification(
            @Parameter(description = "대출 신청 ID", required = true, example = "10") Long applicationId,
            AccountVerificationConfirmRequest request
    );
}
