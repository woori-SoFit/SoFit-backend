package com.sofit.user.domain.loan.dto.response;

public record AccountVerificationResponse(
    String bankName,
    String maskedAccountNumber,
    String accountHolder,
    String expiredAt
) {
}
