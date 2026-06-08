package com.sofit.admin.domain.auth.dto.response;

import com.sofit.common.entity.user.enums.UserRole;

public record AdminMeResponse(
        Long userId,
        String name,
        String loginId,
        String phoneNumber,
        UserRole role
) {
}
