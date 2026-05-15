package com.sofit.user.domain.auth.dto.response;

import java.time.LocalDateTime;

public record FinancialCertVerifyResponse(
        Long certId,
        String certNumber,
        String holderName,
        String phoneNumber,
        String status,
        LocalDateTime verifiedAt
) {
}
