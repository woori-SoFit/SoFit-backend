package com.sofit.admin.domain.auth.converter;

import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
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

    /**
     * 전화번호를 하이픈 포함 형식으로 변환한다.
     * - null/빈 문자열 → 빈 문자열 반환
     * - 11자리 → NNN-NNNN-NNNN 형식
     * - 10자리 → NN-NNNN-NNNN 형식
     * - 그 외 → 원본 그대로 반환
     */
    public static String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "";
        }

        if (phoneNumber.length() == 11) {
            return phoneNumber.substring(0, 3) + "-"
                    + phoneNumber.substring(3, 7) + "-"
                    + phoneNumber.substring(7);
        }

        if (phoneNumber.length() == 10) {
            return phoneNumber.substring(0, 2) + "-"
                    + phoneNumber.substring(2, 6) + "-"
                    + phoneNumber.substring(6);
        }

        return phoneNumber;
    }

    /**
     * User 엔티티를 AdminMeResponse DTO로 변환한다.
     */
    public static AdminMeResponse toMeResponse(User user) {
        return new AdminMeResponse(
                user.getName(),
                user.getLoginId(),
                formatPhoneNumber(user.getPhoneNumber()),
                user.getRole().name()
        );
    }
}
