package com.sofit.user.domain.auth.service;

import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.request.LoginRequest;
import com.sofit.user.domain.auth.dto.request.SignupCompleteRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.CheckLoginIdResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.dto.response.SignupCompleteResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public interface AuthService {

    BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request, HttpSession session);

    void verifyFinancialCertificate(FinancialCertVerifyRequest request, HttpSession session);

    SignupCompleteResponse completeSignup(SignupCompleteRequest request, HttpSession session);

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    CheckLoginIdResponse checkLoginId(String loginId);

    void logout(HttpServletRequest request);
}
