package com.sofit.user.domain.user.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import com.sofit.user.domain.user.service.BusinessService;
import com.sofit.user.global.filter.SessionValidationFilter;
import com.sofit.user.global.util.SecurityUtil;

@WebMvcTest(BusinessController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("BusinessController 단위 테스트")
class BusinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BusinessService businessService;

    @MockitoBean
    private SessionValidationFilter sessionValidationFilter;

    private MockedStatic<SecurityUtil> securityUtilMock;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        securityUtilMock = mockStatic(SecurityUtil.class);
        securityUtilMock.when(SecurityUtil::getCurrentUserId).thenReturn(USER_ID);
    }

    @AfterEach
    void tearDown() {
        securityUtilMock.close();
    }

    @Nested
    @DisplayName("GET /api/businesses/me")
    class FindBusinessProfileTest {

        @Test
        @DisplayName("사업자 정보 조회 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            BusinessProfileResponse response = new BusinessProfileResponse(
                    "1234567890", "홍길동상점", "홍길동", "9001011",
                    LocalDate.of(2020, 1, 1), "음식점업", "한식", "서울시 강남구", true);
            given(businessService.findBusinessProfile(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/businesses/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("BUSINESS2001"))
                    .andExpect(jsonPath("$.result.businessNumber").value("1234567890"))
                    .andExpect(jsonPath("$.result.businessName").value("홍길동상점"))
                    .andExpect(jsonPath("$.result.isMybizConnected").value(true));
        }

        @Test
        @DisplayName("사업자 정보가 없는 경우 404 응답을 반환한다")
        void shouldReturn404WhenNotFound() throws Exception {
            // given
            given(businessService.findBusinessProfile(USER_ID))
                    .willThrow(new BaseException(GeneralErrorCode.NOT_FOUND));

            // when & then
            mockMvc.perform(get("/api/businesses/me"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.isSuccess").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/businesses/me/mybiz-connect")
    class ConnectMybizTest {

        @Test
        @DisplayName("마이 비즈 연동 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            doNothing().when(businessService).connectMybiz(USER_ID);

            // when & then
            mockMvc.perform(post("/api/businesses/me/mybiz-connect"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("BUSINESS2002"));
        }
    }
}
