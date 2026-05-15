package com.sofit.user.domain.auth.dto.request;

public record ExternalFinancialCertRequest(
        String phoneNumber,
        String pin
) {
}
