package com.sofit.user.domain.loan.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "대출 신청 단계", description = "대출 신청 플로우 Step 2~4 래퍼 API")
public interface LoanStepControllerDocs {

    @Operation(summary = "Step 2: 대출 약관 동의", description = "대출 약관에 동의하고 lastCompletedStep을 CONSENT_DONE으로 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "약관 동의 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "DRAFT 상태가 아니거나 단계 순서 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 신청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 건을 찾을 수 없음")
    })
    ApiResponse<ConsentCreateResponse> processConsent(
            @Parameter(description = "대출 신청 ID", required = true, example = "1") Long applicationId,
            ConsentCreateRequest request);

    @Operation(summary = "Step 3: 사업자 정보 확인", description = "사업자 정보를 조회하고 lastCompletedStep을 BIZ_INFO_DONE으로 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사업자 정보 확인 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "DRAFT 상태가 아니거나 단계 순서 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 신청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 건 또는 사업자 정보를 찾을 수 없음")
    })
    ApiResponse<BusinessProfileResponse> processBizInfo(
            @Parameter(description = "대출 신청 ID", required = true, example = "1") Long applicationId);

    @Operation(summary = "Step 4: 마이데이터 약관 동의", description = "마이데이터 약관에 동의하고 lastCompletedStep을 DATA_COLLECTED로 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "마이데이터 약관 동의 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "DRAFT 상태가 아니거나 단계 순서 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 신청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 건을 찾을 수 없음")
    })
    ApiResponse<ConsentCreateResponse> processMydata(
            @Parameter(description = "대출 신청 ID", required = true, example = "1") Long applicationId,
            ConsentCreateRequest request);

    @Operation(summary = "Step 5: 마이비즈데이터 연동 완료", description = "마이비즈데이터 연동 완료를 기록하고 lastCompletedStep을 MYBIZ_CONNECTED로 업데이트합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "마이비즈데이터 연동 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "DRAFT 상태가 아니거나 단계 순서 위반"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 소유가 아닌 신청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신청 건을 찾을 수 없음")
    })
    ApiResponse<Void> processMybizData(
            @Parameter(description = "대출 신청 ID", required = true, example = "1") Long applicationId);
}
