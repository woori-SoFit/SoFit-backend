package com.sofit.admin.domain.auth.dto.response;

public record AdminLoginResponse(
        Long userId,
        String name,
        String role
) {
}
