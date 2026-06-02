package com.sofit.admin.global.util;

import com.sofit.common.entity.user.enums.UserRole;
import org.springframework.stereotype.Service;

/**
 * 현재 인증된 사용자의 역할을 조회하는 재사용 가능한 서비스.
 * SecurityContext의 authorities에서 직접 role을 꺼내어 반환한다.
 */
@Service
public class AdminRoleService {

    /**
     * 현재 인증된 사용자의 역할을 반환한다.
     * SecurityContext에 저장된 authority에서 직접 조회하므로 DB 호출이 없다.
     *
     * @return 현재 사용자의 UserRole
     * @throws com.sofit.common.apiPayload.BaseException SESSION_EXPIRED - 인증 정보 없음
     */
    public UserRole getCurrentUserRole() {
        return SecurityUtil.getCurrentUserRole();
    }
}
