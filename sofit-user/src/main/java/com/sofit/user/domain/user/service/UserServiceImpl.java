package com.sofit.user.domain.user.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.user.converter.UserConverter;
import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserProfileResponse findUser(HttpSession session) {
        // 1. 세션에서 userId 추출
        Object userIdAttr = session.getAttribute("userId");
        if (userIdAttr == null) {
            throw new BaseException(GeneralErrorCode.UNAUTHORIZED);
        }
        Long userId = (userIdAttr instanceof Long) ? (Long) userIdAttr : Long.valueOf(userIdAttr.toString());

        // 2. 사용자 조회 (미존재 시 USER_NOT_FOUND 예외)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(AuthErrorCode.USER_NOT_FOUND));

        // 3. 탈퇴 계정 체크
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BaseException(AuthErrorCode.ACCOUNT_WITHDRAWN);
        }

        // 4. Entity → DTO 변환 후 반환
        return UserConverter.toUserProfileResponse(user);
    }
}
