package com.sofit.user.domain.auth.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.converter.AuthConverter;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.request.LoginRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.ExternalFinancialCertResponse;
import com.sofit.user.domain.auth.dto.response.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.response.ExternalMockApiResponse;
import com.sofit.user.domain.auth.dto.response.FinancialCertVerifyResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ExternalMockClient externalMockClient;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request) {
        ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                externalMockClient.callKycVerify(request.businessNumber());

        if (!mockResponse.isSuccess() || mockResponse.result() == null || !mockResponse.result().isValid()) {
            throw new BaseException(AuthErrorCode.BUSINESS_NOT_FOUND);
        }

        return AuthConverter.toBusinessVerificationResponse(mockResponse.result());
    }

    @Override
    public FinancialCertVerifyResponse verifyFinancialCertificate(FinancialCertVerifyRequest request) {
        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                externalMockClient.callFinancialCertVerify(request.phoneNumber(), request.pin());

        if (!mockResponse.isSuccess()) {
            String code = mockResponse.code();
            if ("AUTH4001".equals(code)) {
                throw new BaseException(AuthErrorCode.PIN_MISMATCH);
            }
            throw new BaseException(AuthErrorCode.CERT_NOT_FOUND);
        }

        return AuthConverter.toFinancialCertVerifyResponse(mockResponse.result());
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpSession session) {
        // 1. loginId로 사용자 조회 (미존재 시 동일 에러)
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BaseException(AuthErrorCode.LOGIN_FAILED));

        // 2. 탈퇴 계정 체크
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BaseException(AuthErrorCode.ACCOUNT_WITHDRAWN);
        }

        // 3. 비밀번호 검증 (불일치 시 동일 에러 — Timing Attack 방지)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BaseException(AuthErrorCode.LOGIN_FAILED);
        }

        // 4. 세션에 사용자 정보 저장
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("role", user.getRole().name());
        session.setAttribute("loginTime", LocalDateTime.now());
        session.setAttribute(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                user.getUserId().toString()
        );

        return new LoginResponse(user.getUserId(), user.getName(), user.getRole().name());
    }
}
