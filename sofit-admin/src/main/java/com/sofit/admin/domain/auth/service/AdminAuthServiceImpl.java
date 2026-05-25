package com.sofit.admin.domain.auth.service;

import com.sofit.admin.domain.auth.converter.AdminAuthConverter;
import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
import com.sofit.admin.domain.auth.exception.AdminAuthErrorCode;
import com.sofit.admin.global.config.LoginAttemptService;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final SessionRegistry sessionRegistry;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @Override
    public AdminLoginResponse login(AdminLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String loginId = request.getLoginId();

        // 0. 브루트포스 방어: 로그인 시도 횟수 초과 시 차단
        if (loginAttemptService.isBlocked(loginId)) {
            throw new BaseException(AdminAuthErrorCode.ACCOUNT_LOCKED);
        }

        // 1. loginId로 User 조회
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> {
                    loginAttemptService.loginFailed(loginId);
                    return new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
                });

        // 2. 비활성 사용자 체크
        if (user.getStatus() == UserStatus.INACTIVE) {
            loginAttemptService.loginFailed(loginId);
            throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
        }

        // 3. 일반 사용자(USER) 접근 차단
        if (user.getRole() == UserRole.USER) {
            loginAttemptService.loginFailed(loginId);
            throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
        }

        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginAttemptService.loginFailed(loginId);
            throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
        }

        // 5. 동시 로그인 제한: 이미 활성 세션이 있으면 차단
        List<SessionInformation> activeSessions = sessionRegistry.getAllSessions(user.getUserId(), false);
        if (!activeSessions.isEmpty()) {
            throw new BaseException(AdminAuthErrorCode.CONCURRENT_LOGIN);
        }

        // 6. 로그인 성공 → 시도 횟수 초기화
        loginAttemptService.loginSucceeded(loginId);

        // 7. SecurityContext 설정 (단일 인증 정보 소스)
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUserId(),
                        null,
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))
                );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        // 8. SessionRegistry에 세션 등록 (동시 로그인 추적용)
        String sessionId = httpRequest.getSession().getId();
        sessionRegistry.registerNewSession(sessionId, user.getUserId());

        // 9. 응답 반환
        return AdminAuthConverter.toLoginResponse(user);
    }

    @Override
    public AdminMeResponse findMe() {
        // 1. SecurityContextHolder에서 userId 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BaseException(AdminAuthErrorCode.SESSION_EXPIRED);
        }

        Long userId;
        try {
            userId = (Long) authentication.getPrincipal();
        } catch (ClassCastException e) {
            throw new BaseException(AdminAuthErrorCode.SESSION_EXPIRED);
        }

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
}
