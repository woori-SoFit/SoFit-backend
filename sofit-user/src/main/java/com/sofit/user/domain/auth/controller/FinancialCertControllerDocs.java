package com.sofit.user.domain.auth.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "금융인증서", description = "범용 금융인증서 본인인증 + PIN 검증 API (대출/마이비즈 등 공통)")
public interface FinancialCertControllerDocs {

    @Operation(summary = "금융인증서 본인인증 + PIN 검증", description = "금융인증서 본인인증과 PIN을 검증합니다. 로그인된 사용자만 호출 가능합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "본인인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "PIN 불일치 또는 입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "금융인증서를 찾을 수 없음")
    })
    ApiResponse<Void> verifyPin(FinancialCertVerifyRequest request);
}
