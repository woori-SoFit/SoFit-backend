package com.sofit.user.domain.loan.dto.response;

public record AccountVerificationResponse(
    String maskedAccountNumber,
    String authCode,
    String expiredAt
) {
}
