package com.sofit.user.domain.auth.service;

import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;

public interface AuthService {

    BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request);
}
