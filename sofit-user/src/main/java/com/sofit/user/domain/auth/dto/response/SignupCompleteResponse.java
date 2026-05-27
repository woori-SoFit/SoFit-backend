package com.sofit.user.domain.auth.dto.response;

public record SignupCompleteResponse(
        Long userId,
        String loginId,
        String name,
        String role
) {
}
