package com.sofit.user.domain.auth.dto.response;

public record LoginResponse(
    Long userId,
    String name,
    String role
) {}
