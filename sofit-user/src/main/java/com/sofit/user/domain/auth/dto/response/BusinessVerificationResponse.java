package com.sofit.user.domain.auth.dto.response;

import java.time.LocalDateTime;

public record BusinessVerificationResponse(
        Long kycId,
        String businessNumber,
        String representativeName,
        String businessName,
        String businessType,
        String openDate,
        boolean isValid,
        LocalDateTime verifiedAt
) {
}
