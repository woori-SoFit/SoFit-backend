package com.sofit.user.domain.auth.dto.response;

import java.time.LocalDateTime;

public record FinancialCertLookupResponse(
        String phoneNumber,
        String certNumber,
        String holderName,
        String status,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt
) {
}
