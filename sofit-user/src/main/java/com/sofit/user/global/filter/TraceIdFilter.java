package com.sofit.user.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 요청 단위 추적 ID(traceId)를 MDC에 심는 필터.
 * - 서버 내부: 모든 로그에 traceId 가 자동으로 붙는다 (logback-spring.xml MDC 포함).
 * - 서버 사이: user ↔ admin 호출 관계가 없으므로 전파 대신 DB 추적 컬럼(trace_id)으로 흔적을 남긴다.
 * - 가장 먼저 실행(HIGHEST_PRECEDENCE)해 이후 모든 필터/요청 로그에 traceId 가 찍히게 한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String SOURCE_SYSTEM = "sourceSystem";
    public static final String CLIENT_IP = "clientIp";
    public static final String ACCESS_METHOD = "accessMethod";
    private static final String TRACE_HEADER = "X-Trace-Id";
    private static final String SYSTEM_NAME = "USER";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        MDC.put(TRACE_ID, traceId);
        MDC.put(SOURCE_SYSTEM, SYSTEM_NAME);
        MDC.put(CLIENT_IP, resolveClientIp(request));
        MDC.put(ACCESS_METHOD, "WEB");
        try {
            response.setHeader(TRACE_HEADER, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
