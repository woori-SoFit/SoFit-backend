package com.sofit.user.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 세션 절대 만료 시간(12시간) 체크 필터
 * - 슬라이딩 만료(30분)는 Spring Session Redis TTL로 자동 관리
 * - 이 필터는 loginAt + 12시간 경과 여부만 체크
 */
@Component
public class SessionValidationFilter extends OncePerRequestFilter {

    private static final long ABSOLUTE_TIMEOUT_HOURS = 12;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 인증 불필요 경로는 필터 스킵
        return path.startsWith("/api/auth/signup")
                || path.equals("/api/auth/login")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);

        if (session != null) {
            LocalDateTime loginTime = (LocalDateTime) session.getAttribute("loginTime");

            if (loginTime != null && loginTime.plusHours(ABSOLUTE_TIMEOUT_HOURS).isBefore(LocalDateTime.now())) {
                // 절대 만료: 세션 무효화 후 401 응답
                session.invalidate();
                sendUnauthorizedResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String body = """
                {"isSuccess":false,"code":"COMMON4001","message":"인증이 필요합니다."}""";
        response.getWriter().write(body);
    }
}
