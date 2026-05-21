package com.sofit.admin.domain.auth.service;

import com.sofit.admin.domain.auth.converter.AdminAuthConverter;
import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
import com.sofit.admin.domain.auth.exception.AdminAuthErrorCode;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AdminLoginResponse login(AdminLoginRequest request, HttpSession session) {
        // 1. loginId로 User 조회
        User user = userRepository.findByLoginId(request.getLoginId())
                .orElseThrow(() -> new BaseException(AdminAuthErrorCode.LOGIN_FAILED));

        // 2. 비활성 사용자 체크
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
        }

        // 3. 일반 사용자(USER) 접근 차단
        if (user.getRole() == UserRole.USER) {
            throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
        }

        // 4. 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BaseException(AdminAuthErrorCode.LOGIN_FAILED);
        }

        // 5. 세션에 사용자 정보 저장
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("role", user.getRole().name());
        session.setAttribute("loginTime", LocalDateTime.now());

        // 6. Spring Security SecurityContext에 Authentication 설정 및 세션에 명시적 저장
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUserId(),
                        null,
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))
                );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

        // 7. 응답 반환
        return AdminAuthConverter.toLoginResponse(user);
    }

    @Override
    public AdminMeResponse findMe(HttpSession session) {
        // 1. 세션에서 userId 추출
        Long userId = (Long) session.getAttribute("userId");

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
