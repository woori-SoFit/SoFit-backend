package com.sofit.user.domain.auth.dto.external;

public record ExternalFinancialCertRequest(
        String phoneNumber,
        String holderName,
        String residentNumber,
        String pin
) {
}
