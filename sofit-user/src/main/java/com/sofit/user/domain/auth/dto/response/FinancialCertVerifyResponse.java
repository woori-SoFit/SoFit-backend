package com.sofit.user.domain.auth.dto.response;

import java.time.LocalDateTime;

public record FinancialCertVerifyResponse(
        String certNumber,
        String holderName,
        String phoneNumber,
        String status,
        LocalDateTime verifiedAt
) {
}
