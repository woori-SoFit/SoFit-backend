package com.sofit.user.domain.auth.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BusinessVerificationResponse(
        String businessNumber,
        String representativeName,
        String businessName,
        String businessType,
        LocalDate openDate,
        LocalDateTime verifiedAt
) {
}
