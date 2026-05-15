package com.sofit.user.domain.auth.service;

import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertVerifyResponse;

public interface AuthService {

    BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request);

    FinancialCertVerifyResponse verifyFinancialCertificate(FinancialCertVerifyRequest request);
}
