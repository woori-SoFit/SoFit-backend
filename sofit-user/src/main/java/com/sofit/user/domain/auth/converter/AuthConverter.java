package com.sofit.user.domain.auth.converter;

import com.sofit.common.entity.auth.RegistrationProcess;
import com.sofit.common.entity.user.User;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.external.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.dto.response.SignupCompleteResponse;

import java.time.LocalDateTime;

public class AuthConverter {

    private AuthConverter() {}

    public static BusinessVerificationResponse toBusinessVerificationResponse(ExternalKycResponse kycResult) {
        return new BusinessVerificationResponse(
                kycResult.businessNumber(),
                kycResult.representativeName(),
                kycResult.businessName(),
                kycResult.businessType(),
                kycResult.openDate(),
                LocalDateTime.now()
        );
    }

    public static BusinessVerificationResponse toBusinessVerificationResponse(RegistrationProcess process) {
        return new BusinessVerificationResponse(
                process.getBusinessNumber(),
                process.getRepresentativeName(),
                process.getBusinessName(),
                process.getBusinessType(),
                process.getOpenDate(),
                process.getCreatedAt()
        );
    }

    public static LoginResponse toLoginResponse(User user) {
        return new LoginResponse(
                user.getUserId(),
                user.getName(),
                user.getRole().name()
        );
    }

    public static SignupCompleteResponse toSignupCompleteResponse(User user) {
        return new SignupCompleteResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getRole().name()
        );
    }
}
