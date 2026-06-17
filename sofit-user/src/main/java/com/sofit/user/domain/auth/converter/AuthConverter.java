package com.sofit.user.domain.auth.converter;

import com.sofit.common.entity.auth.RegistrationProcess;
import com.sofit.common.entity.user.User;
import com.sofit.user.domain.auth.dto.external.ExternalFinancialCertResponse;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.external.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertLookupResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.dto.response.SignupCompleteResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class AuthConverter {

    private AuthConverter() {}

    public static BusinessVerificationResponse toBusinessVerificationResponse(ExternalKycResponse kycResult) {
        LocalDate openDate = (kycResult.openDate() != null && !kycResult.openDate().isBlank())
                ? LocalDate.parse(kycResult.openDate())
                : null;
        return new BusinessVerificationResponse(
                kycResult.businessNumber(),
                kycResult.representativeName(),
                kycResult.businessName(),
                kycResult.businessType(),
                openDate,
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

    public static FinancialCertLookupResponse toFinancialCertLookupResponse(ExternalFinancialCertResponse certResult) {
        return new FinancialCertLookupResponse(
                certResult.phoneNumber(),
                certResult.certNumber(),
                certResult.holderName(),
                certResult.status(),
                parseDateTime(certResult.issuedAt()),
                parseDateTime(certResult.expiresAt())
        );
    }

    private static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr);
    }
}
