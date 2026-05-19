package com.sofit.user.domain.user.converter;

import com.sofit.common.entity.user.User;
import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import com.sofit.user.global.util.MaskingUtil;

public class UserConverter {

    private UserConverter() {}

    /**
     * User 엔티티 → UserProfileResponse 변환
     */
    public static UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getName(),
                user.getLoginId(),
                MaskingUtil.formatPhoneNumber(user.getPhoneNumber()),
                MaskingUtil.maskResidentNumber(user.getResidentNumber())
        );
    }
}
