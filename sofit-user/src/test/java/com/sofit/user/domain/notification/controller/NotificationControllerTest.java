package com.sofit.user.domain.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofit.common.apiPayload.GlobalExceptionHandler;
import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.common.entity.notification.enums.NotificationType;
import com.sofit.user.domain.notification.dto.response.NotificationListResponse;
import com.sofit.user.domain.notification.dto.response.NotificationResponse;
import com.sofit.user.domain.notification.service.NotificationService;
import com.sofit.user.domain.notification.service.SseEmitterManager;
import com.sofit.user.global.filter.SessionValidationFilter;
import com.sofit.user.global.util.SecurityUtil;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("NotificationController 단위 테스트")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private SseEmitterManager sseEmitterManager;

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
    @DisplayName("GET /api/notifications/subscribe")
    class SubscribeTest {

        @Test
        @DisplayName("SSE 구독 성공 시 SseEmitter를 반환한다")
        void shouldReturnSseEmitter() throws Exception {
            // given
            SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
            given(sseEmitterManager.subscribe(USER_ID)).willReturn(emitter);

            // when & then
            mockMvc.perform(get("/api/notifications/subscribe")
                            .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                    .andExpect(status().isOk());

            verify(sseEmitterManager).subscribe(USER_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread")
    class GetUnreadTest {

        @Test
        @DisplayName("미읽음 알림 조회 성공 시 200 응답을 반환한다")
        void shouldReturn200WithUnreadNotifications() throws Exception {
            // given
            NotificationResponse notification = new NotificationResponse(
                    1L, NotificationType.LOAN_SUBMITTED, "대출 신청 완료",
                    "대출 신청이 접수되었습니다.", 100L, "소상공인 대출",
                    false, LocalDateTime.of(2025, 6, 1, 10, 0, 0));
            NotificationListResponse response = new NotificationListResponse(List.of(notification));
            given(notificationService.getUnread(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/notifications/unread"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("NOTI2000"))
                    .andExpect(jsonPath("$.result.notifications").isArray())
                    .andExpect(jsonPath("$.result.notifications[0].notificationId").value(1))
                    .andExpect(jsonPath("$.result.notifications[0].isRead").value(false));
        }

        @Test
        @DisplayName("미읽음 알림이 없으면 빈 리스트를 반환한다")
        void shouldReturn200WithEmptyList() throws Exception {
            // given
            NotificationListResponse response = new NotificationListResponse(Collections.emptyList());
            given(notificationService.getUnread(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/notifications/unread"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.notifications").isEmpty());
        }
    }

    @Nested
    @DisplayName("GET /api/notifications")
    class GetAllTest {

        @Test
        @DisplayName("전체 알림 목록 조회 성공 시 200 응답을 반환한다")
        void shouldReturn200WithAllNotifications() throws Exception {
            // given
            NotificationResponse notification = new NotificationResponse(
                    2L, NotificationType.LOAN_DECIDED, "대출 승인",
                    "대출이 승인되었습니다.", 100L, "소상공인 대출",
                    true, LocalDateTime.of(2025, 6, 2, 14, 0, 0));
            NotificationListResponse response = new NotificationListResponse(List.of(notification));
            given(notificationService.getAll(USER_ID)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/notifications"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("NOTI2003"))
                    .andExpect(jsonPath("$.result.notifications[0].notificationId").value(2))
                    .andExpect(jsonPath("$.result.notifications[0].isRead").value(true));
        }
    }

    @Nested
    @DisplayName("PATCH /api/notifications/{notificationId}/read")
    class MarkAsReadTest {

        @Test
        @DisplayName("알림 읽음 처리 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given
            doNothing().when(notificationService).markAsRead(USER_ID, 5L);

            // when & then
            mockMvc.perform(patch("/api/notifications/5/read"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("NOTI2001"));

            verify(notificationService).markAsRead(USER_ID, 5L);
        }
    }

    @Nested
    @DisplayName("POST /api/notifications/internal/push")
    class PushTest {

        @Test
        @DisplayName("내부 알림 푸시 성공 시 200 응답을 반환한다")
        void shouldReturn200OnSuccess() throws Exception {
            // given — createdAt은 null로 설정 (WebMvcTest에서 JavaTimeModule 미등록 이슈 우회)
            NotificationPushRequest request = NotificationPushRequest.builder()
                    .userId(USER_ID)
                    .notificationId(10L)
                    .type(NotificationType.LOAN_SUBMITTED)
                    .title("대출 신청 완료")
                    .message("대출 신청이 접수되었습니다.")
                    .referenceId(100L)
                    .referenceLabel("소상공인 대출")
                    .isRead(false)
                    .build();

            doNothing().when(notificationService).push(any(NotificationPushRequest.class));

            // when & then
            mockMvc.perform(post("/api/notifications/internal/push")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("NOTI2002"));

            verify(notificationService).push(any(NotificationPushRequest.class));
        }

        @Test
        @DisplayName("필수 필드 누락 시 400 응답을 반환한다")
        void shouldReturn400WhenRequiredFieldMissing() throws Exception {
            // given — userId, notificationId, type, referenceId가 null
            String invalidBody = "{}";

            // when & then
            mockMvc.perform(post("/api/notifications/internal/push")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest());
        }
    }
}
