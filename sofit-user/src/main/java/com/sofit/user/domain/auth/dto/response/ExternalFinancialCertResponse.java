package com.sofit.user.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalFinancialCertResponse(
        String phoneNumber,
        String certNumber,
        String holderName,
        String status,
        String issuedAt,
        String expiresAt
) {
}
