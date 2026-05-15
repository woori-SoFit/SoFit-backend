package com.sofit.user.domain.auth.converter;

import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.ExternalKycResponse;

import java.time.LocalDateTime;

public class AuthConverter {

    private AuthConverter() {}

    public static BusinessVerificationResponse toBusinessVerificationResponse(ExternalKycResponse kycResult) {
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
}
