package com.sofit.admin.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 권한 부족 시 JSON 형식의 공통 에러 응답을 반환하는 Handler.
 * Spring Security 기본 응답 대신 일관된 JSON 에러 포맷을 제공한다.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        GeneralErrorCode errorCode = GeneralErrorCode.FORBIDDEN;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("isSuccess", false);
        body.put("code", errorCode.getCode());
        body.put("message", errorCode.getMessage());
        body.put("result", null);

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
