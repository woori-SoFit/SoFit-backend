package com.sofit.admin.global.util;

import com.sofit.admin.domain.auth.exception.AdminAuthErrorCode;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.enums.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext에서 인증 정보를 추출하는 유틸리티 클래스.
 * - 로그인 시 Authentication.principal에 userId(Long)를 저장하는 구조 전제
 * - Authentication.authorities에 role(UserRole.name())을 저장하는 구조 전제
 */
public class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * 현재 인증된 사용자의 userId를 반환한다.
     * 인증 정보가 없으면 SESSION_EXPIRED 예외를 던진다.
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new BaseException(AdminAuthErrorCode.SESSION_EXPIRED);
        }

        Object principal = authentication.getPrincipal();
        if ("anonymousUser".equals(principal)) {
            throw new BaseException(AdminAuthErrorCode.SESSION_EXPIRED);
        }

        try {
            return (Long) principal;
        } catch (ClassCastException e) {
            throw new BaseException(AdminAuthErrorCode.SESSION_EXPIRED);
        }
    }

    /**
     * 현재 인증된 사용자가 있는지 여부를 반환한다.
     * 예외를 던지지 않고 boolean으로 판단할 때 사용.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * SecurityContext의 authorities에서 현재 사용자의 역할(UserRole)을 반환한다.
     * DB 조회 없이 SecurityContext만으로 role을 확인할 때 사용한다.
     *
     * @return 현재 사용자의 UserRole
     * @throws BaseException SESSION_EXPIRED - 인증 정보 없음 또는 authority 없음
     */
    public static UserRole getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null || authentication.getAuthorities().isEmpty()) {
            throw new BaseException(AdminAuthErrorCode.SESSION_EXPIRED);
        }

        String authority = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElseThrow(() -> new BaseException(AdminAuthErrorCode.SESSION_EXPIRED));

        try {
            return UserRole.valueOf(authority);
        } catch (IllegalArgumentException e) {
            throw new BaseException(AdminAuthErrorCode.SESSION_EXPIRED);
        }
    }

    /**
     * 현재 인증된 사용자가 특정 권한을 보유하고 있는지 확인한다.
     */
    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(authority));
    }
}
