package com.sofit.user.domain.auth.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.auth.client.ExternalMockClient;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.external.ExternalMockApiResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialCertServiceImpl implements FinancialCertService {

    private final ExternalMockClient externalMockClient;

    @Override
    public void verify(FinancialCertVerifyRequest request) {
        ExternalMockApiResponse<Void> mockResponse =
                externalMockClient.callFinancialCertIdentityVerify(
                        request.getPhoneNumber(),
                        request.getHolderName(),
                        request.getResidentNumber(),
                        request.getPin()
                );

        if (!mockResponse.isSuccess()) {
            String code = mockResponse.code();
            if ("AUTH4001".equals(code)) {
                throw new BaseException(AuthErrorCode.PIN_MISMATCH);
            }
            throw new BaseException(AuthErrorCode.CERT_VERIFICATION_FAILED);
        }
    }
}
