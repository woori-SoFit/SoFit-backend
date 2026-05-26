package com.sofit.user.domain.auth.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.auth.converter.AuthConverter;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.ExternalFinancialCertResponse;
import com.sofit.user.domain.auth.dto.response.ExternalMockApiResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertVerifyResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 금융인증서 PIN 인증만 담당하는 순수 서비스.
 * 세션, HTTP, Spring Security 등 웹 레이어 의존성 없음.
 * 회원가입/대출신청/마이비즈 등 어느 플로우에서든 재사용 가능.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialCertService {

    private final ExternalMockClient externalMockClient;

    /**
     * 금융인증서 PIN 인증을 수행한다.
     * 성공 시 FinancialCertVerifyResponse를 반환하고, 실패 시 예외를 던진다.
     */
    public FinancialCertVerifyResponse verify(FinancialCertVerifyRequest request) {
        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                externalMockClient.callFinancialCertVerify(request.getPhoneNumber(), request.getPin());

        if (!mockResponse.isSuccess()) {
            String code = mockResponse.code();
            if ("AUTH4001".equals(code)) {
                throw new BaseException(AuthErrorCode.PIN_MISMATCH);
            }
            throw new BaseException(AuthErrorCode.CERT_NOT_FOUND);
        }

        ExternalFinancialCertResponse certResult = mockResponse.result();

        if (!"VALID".equals(certResult.status())) {
            throw new BaseException(AuthErrorCode.CERT_VERIFICATION_FAILED);
        }

        return AuthConverter.toFinancialCertVerifyResponse(certResult);
    }
}
