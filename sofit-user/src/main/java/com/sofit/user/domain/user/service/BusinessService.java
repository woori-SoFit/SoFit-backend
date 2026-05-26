package com.sofit.user.domain.user.service;

import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;

public interface BusinessService {

    BusinessProfileResponse findBusinessProfile(Long userId);

    void connectMybiz(Long userId);
}
