package com.sofit.user.domain.mybiz.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.user.domain.mybiz.service.MyBizService;
import com.sofit.user.global.filter.SessionValidationFilter;

@WebMvcTest(MyBizController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MyBizControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MyBizService myBizService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUpSecurityContext() {
        var authentication = new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("month 파라미터 없이 대시보드 조회 시 200 + MYBIZ2001 응답을 반환한다")
    void findDashboard_withoutMonth_returns200() throws Exception {
        // given
        given(myBizService.findDashboard(eq(USER_ID), isNull())).willReturn(null);

        // when & then
        mockMvc.perform(get("/api/mybiz/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("MYBIZ2001"));
    }

    @Test
    @DisplayName("month 파라미터를 전달하면 서비스로 전달되고 200을 반환한다")
    void findDashboard_withMonth_returns200() throws Exception {
        // given
        given(myBizService.findDashboard(eq(USER_ID), eq("2026-05"))).willReturn(null);

        // when & then
        mockMvc.perform(get("/api/mybiz/dashboard").param("month", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("MYBIZ2001"));
    }
}
