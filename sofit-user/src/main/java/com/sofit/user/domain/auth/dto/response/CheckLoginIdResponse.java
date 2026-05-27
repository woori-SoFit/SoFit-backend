package com.sofit.user.domain.auth.dto.response;

public record CheckLoginIdResponse(
        String loginId,
        boolean available
) {
}
