package com.sofit.user.global.util;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * SecurityContext에서 인증 정보를 추출하는 유틸리티 클래스.
 * - 로그인 시 Authentication.principal에 userId(Long)를 저장하는 구조 전제
 */
public class SecurityUtil {

    private SecurityUtil() {
    }

    /**
     * 현재 인증된 사용자의 userId를 반환한다.
     * 인증 정보가 없으면 UNAUTHORIZED 예외를 던진다.
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new BaseException(GeneralErrorCode.UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            return (Long) principal;
        }
        return Long.valueOf(principal.toString());
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
}
