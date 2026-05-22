package com.sofit.admin.domain.auth.converter;

import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
import com.sofit.common.entity.user.User;
import com.sofit.common.util.FormatUtil;

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

    /**
     * User 엔티티를 AdminMeResponse DTO로 변환한다.
     */
    public static AdminMeResponse toMeResponse(User user) {
        return new AdminMeResponse(
                user.getName(),
                user.getLoginId(),
                FormatUtil.formatPhoneNumber(user.getPhoneNumber()),
                user.getRole()
        );
    }
}
