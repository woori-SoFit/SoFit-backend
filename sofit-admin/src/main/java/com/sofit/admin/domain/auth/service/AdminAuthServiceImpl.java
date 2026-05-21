package com.sofit.admin.domain.auth.service;

import com.sofit.admin.domain.auth.converter.AdminAuthConverter;
import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.exception.AdminAuthErrorCode;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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

        // 6. 응답 반환
        return AdminAuthConverter.toLoginResponse(user);
    }
}
