package com.sofit.user.domain.auth.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.auth.dto.request.ExternalFinancialCertRequest;
import com.sofit.user.domain.auth.dto.request.ExternalKycRequest;
import com.sofit.user.domain.auth.dto.response.ExternalFinancialCertResponse;
import com.sofit.user.domain.auth.dto.response.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.response.ExternalMockApiResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExternalMockClient {

    private final RestClient externalMockRestClient;

    /**
     * External Mock 서버에 사업자등록번호 진위 확인 요청
     */
    public ExternalMockApiResponse<ExternalKycResponse> callKycVerify(String businessNumber) {
        try {
            return externalMockRestClient.post()
                    .uri("/ext/kyc/verify")
                    .body(new ExternalKycRequest(businessNumber))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        // 4xx는 Mock 서버가 돌려준 비즈니스 응답 → 그대로 파싱
                    })
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            log.error("[ExternalMockClient] KYC 호출 실패: {}", e.getMessage(), e);
            throw new BaseException(AuthErrorCode.EXTERNAL_SERVER_ERROR);
        }
    }

    /**
     * External Mock 서버에 금융인증서 PIN 인증 요청
     */
    public ExternalMockApiResponse<ExternalFinancialCertResponse> callFinancialCertVerify(String phoneNumber, String pin) {
        try {
            return externalMockRestClient.post()
                    .uri("/ext/financial-certs/verify")
                    .body(new ExternalFinancialCertRequest(phoneNumber, pin))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        // 4xx는 Mock 서버가 돌려준 비즈니스 응답 → 그대로 파싱
                    })
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            log.error("[ExternalMockClient] 금융인증서 호출 실패: {}", e.getMessage(), e);
            throw new BaseException(AuthErrorCode.EXTERNAL_SERVER_ERROR);
        }
    }
}
