package com.sofit.admin.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 세션 무효화 + SecurityContext 클리어 + 쿠키 만료 공통 유틸.
 * 로그아웃 등에서 재사용한다.
 */
public class SessionUtil {

    private SessionUtil() {}

    /**
     * 현재 요청의 세션을 무효화하고, SecurityContext를 클리어하고, 세션 쿠키를 만료시킨다.
     */
    public static void invalidateSession(HttpServletRequest request, HttpServletResponse response) {
        // 1. 현재 세션 무효화
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // 2. SecurityContext 클리어
        SecurityContextHolder.clearContext();

        // 3. 세션 쿠키 만료 처리 (Spring Session + 톰캣 기본 쿠키 모두 제거)
        boolean isSecure = request.isSecure();

        Cookie sessionCookie = new Cookie("SESSION", null);
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setSecure(isSecure);
        sessionCookie.setMaxAge(0);
        response.addCookie(sessionCookie);

        Cookie jsessionIdCookie = new Cookie("JSESSIONID", null);
        jsessionIdCookie.setPath("/");
        jsessionIdCookie.setHttpOnly(true);
        jsessionIdCookie.setSecure(isSecure);
        jsessionIdCookie.setMaxAge(0);
        response.addCookie(jsessionIdCookie);
    }
}
