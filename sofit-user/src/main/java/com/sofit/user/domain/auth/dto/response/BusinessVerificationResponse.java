package com.sofit.user.domain.auth.dto.response;

import java.time.LocalDateTime;

public record BusinessVerificationResponse(
        String registrationId,
        String businessNumber,
        String representativeName,
        String businessName,
        String businessType,
        String openDate,
        LocalDateTime verifiedAt
) {
}
