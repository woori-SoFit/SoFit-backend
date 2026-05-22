package com.sofit.user.domain.user.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.user.converter.UserConverter;
import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @Override
    public UserProfileResponse findUser(Long userId) {
        // 1. 사용자 조회 (미존재 시 USER_NOT_FOUND 예외)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(AuthErrorCode.USER_NOT_FOUND));

        // 2. 탈퇴 계정 체크
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BaseException(AuthErrorCode.ACCOUNT_WITHDRAWN);
        }

        // 3. Entity → DTO 변환 후 반환
        return UserConverter.toUserProfileResponse(user);
    }

    @Override
    @Transactional
    public void withdraw(Long userId, HttpServletRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(AuthErrorCode.USER_NOT_FOUND));

        // 2. Soft Delete (status=INACTIVE, inactivatedAt 기록)
        user.inactivate();

        // 3. 해당 사용자의 모든 활성 세션 삭제 (Redis에서 역조회)
        Map<String, ? extends Session> userSessions =
                sessionRepository.findByPrincipalName(userId.toString());
        userSessions.keySet().forEach(sessionRepository::deleteById);

        // 4. SecurityContext 클리어
        SecurityContextHolder.clearContext();

        // 5. 현재 세션 무효화
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
