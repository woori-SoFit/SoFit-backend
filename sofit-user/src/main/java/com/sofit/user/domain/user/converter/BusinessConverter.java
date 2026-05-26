package com.sofit.user.domain.user.converter;

import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.user.User;
import com.sofit.common.util.FormatUtil;
import com.sofit.common.util.MaskingUtil;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;

public class BusinessConverter {

    private BusinessConverter() {}

    /**
     * BusinessProfile 엔티티 → BusinessProfileResponse 변환
     */
    public static BusinessProfileResponse toBusinessProfileResponse(BusinessProfile profile) {
        User user = profile.getUser();
        return new BusinessProfileResponse(
                FormatUtil.formatBusinessNumber(profile.getBusinessNumber()),
                profile.getBusinessName(),
                profile.getRepresentativeName(),
                MaskingUtil.maskResidentNumber(user.getResidentNumber()),
                profile.getOpenDate(),
                profile.getBusinessCategory(),
                profile.getBusinessType(),
                profile.getBusinessAddress(),
                profile.isMybizConnected()
        );
    }
}
