package com.sofit.user.domain.terms.controller;

import org.springframework.web.bind.annotation.RequestBody;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "약관 동의", description = "약관 동의 이력 저장 API")
public interface TermConsentControllerDocs {

    @Operation(summary = "약관 동의 저장", description = "사용자의 약관 동의 이력을 저장합니다. 필수 약관 미동의 시 요청이 거부됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "약관 동의 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패, 약관 유형 불일치, 필수 약관 미동의)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 약관 또는 대출 신청")
    })
    ApiResponse<ConsentCreateResponse> createConsents(
            @Valid @RequestBody ConsentCreateRequest request);
}
