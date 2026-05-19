package com.sofit.user.domain.user.dto.response;

public record UserProfileResponse(
    String name,
    String username,
    String phoneNumber,
    String residentNumber
) {}
