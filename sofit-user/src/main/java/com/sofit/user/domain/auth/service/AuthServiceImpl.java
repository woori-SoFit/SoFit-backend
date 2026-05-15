package com.sofit.user.domain.auth.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.auth.converter.AuthConverter;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.response.ExternalMockApiResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ExternalMockClient externalMockClient;

    @Override
    public BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request) {
        ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                externalMockClient.callKycVerify(request.businessNumber());

        if (!mockResponse.isSuccess() || mockResponse.result() == null || !mockResponse.result().isValid()) {
            throw new BaseException(AuthErrorCode.BUSINESS_NOT_FOUND);
        }

        return AuthConverter.toBusinessVerificationResponse(mockResponse.result());
    }
}
