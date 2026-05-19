package com.sofit.user.domain.user.dto.response;

public record UserProfileResponse(
    String name,
    String loginId,
    String phoneNumber,
    String residentNumber
) {}
