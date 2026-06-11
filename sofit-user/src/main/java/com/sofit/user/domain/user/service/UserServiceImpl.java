package com.sofit.user.domain.user.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.user.converter.UserConverter;
import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import com.sofit.user.domain.user.event.UserWithdrawnEvent;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import com.sofit.common.audit.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

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
    @AuditLog(action = "WITHDRAW", target = "회원 탈퇴")
    public void withdraw(Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(AuthErrorCode.USER_NOT_FOUND));

        // 2. Soft Delete (status=INACTIVE, inactivatedAt 기록)
        user.inactivate();
        log.info("회원 탈퇴 userId={}", userId);

        // 3. DB 커밋 완료 후 세션 삭제를 위한 이벤트 발행
        eventPublisher.publishEvent(new UserWithdrawnEvent(userId));
    }
}
