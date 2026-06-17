package com.sofit.admin.domain.dev.dto.response;

import java.time.LocalDateTime;

public record UserItemResponse(
        Long id,
        String loginId,
        String name,
        String role,
        String status,
        String phoneNumber,
        LocalDateTime createdAt
) {
}
