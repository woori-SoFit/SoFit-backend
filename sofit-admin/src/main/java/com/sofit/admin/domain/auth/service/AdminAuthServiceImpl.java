package com.sofit.admin.domain.auth.service;

import com.sofit.admin.domain.auth.converter.AdminAuthConverter;
import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
import com.sofit.admin.domain.auth.exception.AdminAuthErrorCode;
import com.sofit.admin.domain.auth.service.LoginAttemptService;
import com.sofit.admin.global.util.SecurityUtil;
import com.sofit.admin.global.util.SessionUtil;
import com.sofit.common.logging.LogMaskUtil;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.audit.AuditLog;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    @AuditLog(action = "LOGIN", target = "관리자 로그인")
    public AdminLoginResponse login(AdminLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String loginId = request.getLoginId();
        String ipAddress = getClientIp(httpRequest);

        // 0. 브루트포스 방어: IP 또는 계정 잠금 시 차단
        if (loginAttemptService.isBlocked(loginId, ipAddress)) {
            throw new BaseException(AdminAuthErrorCode.ACCOUNT_LOCKED);
        }

        // 1. loginId로 User 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> {
                    log.warn("관리자 로그인 실패 loginId={} ip={}", LogMaskUtil.maskLoginId(loginId), ipAddress);
                    loginAttemptService.loginFailed(loginId, ipAddress);
                    return new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
                });

        // 2. 비활성 사용자 체크
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("관리자 로그인 실패 loginId={} ip={}", LogMaskUtil.maskLoginId(loginId), ipAddress);
            loginAttemptService.loginFailed(loginId, ipAddress);
            throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
        }

        // 3. 일반 사용자(USER) 접근 차단
        if (user.getRole() == UserRole.USER) {
            log.warn("관리자 페이지 일반 사용자 접근 시도 loginId={} ip={}", LogMaskUtil.maskLoginId(loginId), ipAddress);
            loginAttemptService.loginFailed(loginId, ipAddress);
            throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
        }

        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("관리자 로그인 실패 loginId={} ip={}", LogMaskUtil.maskLoginId(loginId), ipAddress);
            loginAttemptService.loginFailed(loginId, ipAddress);
            throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
        }

        // 5. 로그인 성공 → 시도 횟수 초기화
        loginAttemptService.loginSucceeded(loginId);

        // 6. SecurityContext 설정 (단일 인증 정보 소스)
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUserId(),
                        null,
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))
                );

        // 7. 동시 로그인 제한 — Spring Security SessionAuthenticationStrategy에 위임
        // 세션 고정 공격 방지 + 세션 등록 + 동시 세션 제한을 일관되게 처리
        try {
            sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
        } catch (SessionAuthenticationException e) {
            throw new BaseException(AdminAuthErrorCode.CONCURRENT_LOGIN);
        }

        // 8. SecurityContext 저장
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);
        log.info("관리자 로그인 role={}", user.getRole());

        // 9. 응답 반환
        return AdminAuthConverter.toLoginResponse(user);
    }

    @Override
    public AdminMeResponse findMe() {
        // 1. SecurityUtil에서 userId 추출
        Long userId = SecurityUtil.getCurrentUserId();

        // 2. UserRepository로 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(AdminAuthErrorCode.USER_NOT_FOUND));

        // 3. 사용자 상태가 INACTIVE이면 예외 처리
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BaseException(AdminAuthErrorCode.USER_NOT_FOUND);
        }

        // 4. 응답 반환
        return AdminAuthConverter.toMeResponse(user);
    }

    @Override
    @AuditLog(action = "LOGOUT", target = "관리자 로그아웃")
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        SessionUtil.invalidateSession(httpRequest, httpResponse);
        log.info("관리자 로그아웃");
    }

    /**
     * 클라이언트 IP를 추출한다.
     * 프록시/로드밸런서 뒤에 있을 경우 X-Forwarded-For 헤더를 우선 사용한다.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // 여러 프록시를 거친 경우 첫 번째가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
