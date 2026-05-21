package com.sofit.admin.domain.auth.dto.response;

public record AdminMeResponse(
        String name,
        String loginId,
        String phoneNumber,
        String role
) {
}
