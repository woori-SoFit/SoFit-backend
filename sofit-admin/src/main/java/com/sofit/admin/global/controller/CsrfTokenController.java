package com.sofit.admin.global.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.code.GeneralSuccessCode;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CSRF 토큰 발급 엔드포인트.
 * 프론트엔드는 페이지 로드 시 이 엔드포인트를 호출하여 토큰을 받고,
 * 이후 상태 변경 요청(POST/PUT/DELETE/PATCH)의 X-CSRF-TOKEN 헤더에 포함하여 전송한다.
 */
@RestController
public class CsrfTokenController {

    @GetMapping("/api/admin/csrf-token")
    public ApiResponse<CsrfTokenResponse> getCsrfToken(CsrfToken csrfToken) {
        CsrfTokenResponse response = new CsrfTokenResponse(
                csrfToken.getToken(),
                csrfToken.getHeaderName()
        );
        return ApiResponse.onSuccess(GeneralSuccessCode.OK, response);
    }

    public record CsrfTokenResponse(
            String token,
            String headerName
    ) {}
}
