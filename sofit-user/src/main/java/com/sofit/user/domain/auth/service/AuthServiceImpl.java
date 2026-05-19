package com.sofit.user.domain.auth.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.RegistrationProcess;
import com.sofit.common.entity.auth.enums.RegistrationStep;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.UserStatus;
import com.sofit.common.repository.auth.RegistrationProcessRepository;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ExternalMockClient externalMockClient;
    private final UserRepository userRepository;
    private final RegistrationProcessRepository registrationProcessRepository;
    private final PasswordEncoder passwordEncoder;
    private final HttpSessionSecurityContextRepository securityContextRepository;

    @Override
    public BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request, HttpSession session) {
        // 1. 세션에 이미 프로세스가 있으면 기존 결과 반환 (중복 요청 방지)
        Long existingProcessId = (Long) session.getAttribute("registrationProcessId");
        if (existingProcessId != null) {
            RegistrationProcess existing = registrationProcessRepository.findById(existingProcessId)
                    .orElse(null);
            if (existing != null) {
                return AuthConverter.toBusinessVerificationResponse(existing);
            }
        }

        // 2. External Mock 호출
        ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                externalMockClient.callKycVerify(request.getBusinessNumber());

        if (!mockResponse.isSuccess() || mockResponse.result() == null || !mockResponse.result().isValid()) {
            throw new BaseException(AuthErrorCode.BUSINESS_NOT_FOUND);
        }

        ExternalKycResponse kycResult = mockResponse.result();

        // 3. 기존 RegistrationProcess가 있으면 업데이트, 없으면 신규 생성
        RegistrationProcess process = registrationProcessRepository
                .findByBusinessNumber(kycResult.businessNumber())
                .map(existing -> {
                    existing.updateKycResult(
                            kycResult.businessNumber(),
                            kycResult.businessName(),
                            kycResult.representativeName(),
                            kycResult.openDate(),
                            kycResult.businessType()
                    );
                    return existing;
                })
                .orElseGet(() -> RegistrationProcess.createForStep1(
                        kycResult.businessNumber(),
                        kycResult.businessName(),
                        kycResult.representativeName(),
                        kycResult.openDate(),
                        kycResult.businessType()
                ));
        registrationProcessRepository.save(process);

        // 4. 세션에 PK 저장
        session.setAttribute("registrationProcessId", process.getId());

        // 5. 응답 반환
        return AuthConverter.toBusinessVerificationResponse(kycResult);
    }

    @Override
    @Transactional
    public FinancialCertVerifyResponse verifyFinancialCertificate(FinancialCertVerifyRequest request, HttpSession session) {
        // 1. 세션에서 registrationProcessId 조회
        Long processId = (Long) session.getAttribute("registrationProcessId");
        if (processId == null) {
            throw new BaseException(AuthErrorCode.REGISTRATION_EXPIRED);
        }

        RegistrationProcess process = registrationProcessRepository.findById(processId)
                .orElseThrow(() -> new BaseException(AuthErrorCode.REGISTRATION_EXPIRED));

        // 2. 만료 체크 (updated_at + 30분)
        if (process.getUpdatedAt().plusMinutes(30).isBefore(LocalDateTime.now())) {
            process.expire();
            registrationProcessRepository.save(process);
            throw new BaseException(AuthErrorCode.REGISTRATION_EXPIRED);
        }

        // 3. Step 1 완료 여부 확인
        if (process.getStep() != RegistrationStep.STEP_1_COMPLETED) {
            throw new BaseException(AuthErrorCode.STEP_NOT_COMPLETED);
        }

        // 4. External Mock 서버에 PIN 인증 요청
        ExternalMockApiResponse<ExternalFinancialCertResponse> mockResponse =
                externalMockClient.callFinancialCertVerify(request.getPhoneNumber(), request.getPin());

        if (!mockResponse.isSuccess()) {
            String code = mockResponse.code();
            if ("AUTH4001".equals(code)) {
                throw new BaseException(AuthErrorCode.PIN_MISMATCH);
            }
            throw new BaseException(AuthErrorCode.CERT_NOT_FOUND);
        }

        ExternalFinancialCertResponse certResult = mockResponse.result();

        // 5. 금융인증서 상태 VALID 확인
        if (!"VALID".equals(certResult.status())) {
            throw new BaseException(AuthErrorCode.CERT_VERIFICATION_FAILED);
        }

        // 6. 성공 시 RegistrationProcess 갱신
        process.completeStep2();
        registrationProcessRepository.save(process);

        return AuthConverter.toFinancialCertVerifyResponse(certResult);
    }

    @Override
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
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

        // 4. SecurityContext에 인증 정보 저장
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUserId(), null,
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))
                );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 5. HttpSessionSecurityContextRepository를 통해 세션에 영속화
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        // 6. 세션에 사용자 정보 저장
        HttpSession session = httpRequest.getSession();
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("role", user.getRole().name());
        session.setAttribute("loginTime", LocalDateTime.now());
        session.setAttribute(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                user.getUserId().toString()
        );

        return AuthConverter.toLoginResponse(user);
    }
}
