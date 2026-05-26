package com.sofit.user.domain.user.dto.response;

import java.time.LocalDate;

public record BusinessProfileResponse(
    String businessNumber,
    String businessName,
    String representativeName,
    String residentNumber,
    LocalDate openDate,
    String businessCategory,
    String businessType,
    String businessAddress,
    boolean isMybizConnected
) {}
