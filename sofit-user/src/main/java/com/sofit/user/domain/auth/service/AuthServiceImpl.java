package com.sofit.user.domain.auth.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.ExternalFinancialCertResponse;
import com.sofit.user.domain.auth.dto.response.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.response.ExternalMockApiResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertVerifyResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ExternalMockClient externalMockClient;

    @Override
    public BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request) {
        // 1. External Mock 서버에 KYC 인증 요청
        ExternalMockApiResponse<ExternalKycResponse> mockResponse = externalMockClient.callKycVerify(request.businessNumber());

        // 2. 유효하지 않은 사업자 (폐업/미등록) 처리
        if (!mockResponse.isSuccess() || mockResponse.result() == null || !mockResponse.result().isValid()) {
            throw new BaseException(AuthErrorCode.BUSINESS_NOT_FOUND);
        }

        ExternalKycResponse kycResult = mockResponse.result();

        // 3. 인증 결과 반환 (DB 저장 없음 — 세션에 저장은 Controller에서 처리)
        return new BusinessVerificationResponse(
                null,
                kycResult.businessNumber(),
                kycResult.representativeName(),
                kycResult.businessName(),
                kycResult.businessType(),
                kycResult.openDate(),
                true,
                LocalDateTime.now()
        );
    }

    @Override
    public FinancialCertVerifyResponse verifyFinancialCertificate(FinancialCertVerifyRequest request) {
        // 1. External Mock 서버에 PIN 인증 요청
        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                externalMockClient.callFinancialCertVerify(request.phoneNumber(), request.pin());

        // 2. 실패 처리
        if (!mockResponse.isSuccess()) {
            String code = mockResponse.code();
            if ("AUTH4001".equals(code)) {
                throw new BaseException(AuthErrorCode.PIN_MISMATCH);
            }
            throw new BaseException(AuthErrorCode.CERT_NOT_FOUND);
        }

        ExternalFinancialCertResponse certResult = mockResponse.result();

        // 3. 인증 결과 반환 (DB 저장 없음)
        return new FinancialCertVerifyResponse(
                null,
                certResult.certNumber(),
                certResult.holderName(),
                certResult.phoneNumber(),
                certResult.status(),
                LocalDateTime.now()
        );
    }
}
