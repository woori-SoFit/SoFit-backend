package com.sofit.user.global.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SessionValidationFilter extends OncePerRequestFilter {

    private static final long ABSOLUTE_TIMEOUT_HOURS = 12;
    private final ObjectMapper objectMapper;

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
                response.setStatus(GeneralErrorCode.UNAUTHORIZED.getHttpStatus().value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getOutputStream(),
                        ApiResponse.onFailure(GeneralErrorCode.UNAUTHORIZED));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
