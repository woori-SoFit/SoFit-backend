package com.sofit.admin.global.util;

import com.sofit.admin.domain.auth.exception.AdminAuthErrorCode;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 현재 인증된 사용자의 역할을 조회하는 재사용 가능한 서비스.
 * SecurityUtil과 UserRepository를 조합하여 역할을 반환한다.
 */
@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final UserRepository userRepository;

    /**
     * 현재 인증된 사용자의 역할을 반환한다.
     *
     * @return 현재 사용자의 UserRole
     * @throws BaseException SESSION_EXPIRED - 인증 정보 없음 (SecurityUtil에서 발생)
     * @throws BaseException USER_NOT_FOUND - 사용자 미존재 또는 INACTIVE
     */
    public UserRole getCurrentUserRole() {
        Long userId = SecurityUtil.getCurrentUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(AdminAuthErrorCode.USER_NOT_FOUND));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BaseException(AdminAuthErrorCode.USER_NOT_FOUND);
        }

        return user.getRole();
    }
}
