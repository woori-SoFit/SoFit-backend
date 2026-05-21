package com.sofit.admin.domain.auth.converter;

import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.common.entity.user.User;

public class AdminAuthConverter {

    private AdminAuthConverter() {
    }

    public static AdminLoginResponse toLoginResponse(User user) {
        return new AdminLoginResponse(
                user.getUserId(),
                user.getName(),
                user.getRole().name()
        );
    }
}
