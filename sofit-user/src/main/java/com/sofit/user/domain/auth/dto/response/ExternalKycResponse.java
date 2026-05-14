package com.sofit.user.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExternalKycResponse(
        String businessNumber,
        String representativeName,
        String businessCategory,
        String businessType,
        String businessName,
        String businessAddress,
        String openDate,
        boolean isValid
) {
}
