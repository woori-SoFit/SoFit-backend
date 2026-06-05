package com.sofit.user.domain.auth.dto.response;

public record FinancialCertLookupResponse(
        String phoneNumber,
        String certNumber,
        String holderName,
        String status,
        String issuedAt,
        String expiresAt
) {
}
