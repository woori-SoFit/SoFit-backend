package com.sofit.user.domain.notification.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.notification.Notification;
import com.sofit.common.entity.notification.enums.NotificationType;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.notification.NotificationRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.notification.dto.response.NotificationListResponse;
import com.sofit.user.domain.notification.exception.NotificationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl 단위 테스트")
class NotificationServiceImplTest {

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private SseEmitterManager sseEmitterManager;

    private static final Long USER_ID = 1L;
    private static final Long APPLICATION_ID = 10L;

    @Nested
    @DisplayName("send")
    class SendTest {

        @Test
        @DisplayName("알림을 정상 생성하고 SSE 푸시한다")
        void shouldSendNotificationSuccessfully() {
            // given
            User user = createUser(USER_ID);
            LoanApplication application = createLoanApplication(APPLICATION_ID, "소상공인 대출");

            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));
            given(notificationRepository.save(any(Notification.class))).willAnswer(inv -> inv.getArgument(0));

            // when
            notificationService.send(USER_ID, NotificationType.LOAN_DECIDED, APPLICATION_ID);

            // then
            verify(notificationRepository).save(any(Notification.class));
            verify(sseEmitterManager).send(any(), any(NotificationPushRequest.class));
        }

        @Test
        @DisplayName("사용자 미존재 시 예외를 던진다")
        void shouldThrowWhenUserNotFound() {
            // given
            given(userRepository.findById(USER_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.send(USER_ID, NotificationType.LOAN_DECIDED, APPLICATION_ID))
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("대출 신청 미존재 시 예외를 던진다")
        void shouldThrowWhenApplicationNotFound() {
            // given
            User user = createUser(USER_ID);
            given(userRepository.findById(USER_ID)).willReturn(Optional.of(user));
            given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.send(USER_ID, NotificationType.LOAN_DECIDED, APPLICATION_ID))
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("push")
    class PushTest {

        @Test
        @DisplayName("SSE 푸시를 정상 전달한다")
        void shouldPushNotification() {
            // given
            NotificationPushRequest request = NotificationPushRequest.builder()
                    .userId(USER_ID)
                    .notificationId(1L)
                    .type(NotificationType.LOAN_DECIDED)
                    .title("대출 심사 완료")
                    .message("심사가 완료되었습니다.")
                    .referenceId(APPLICATION_ID)
                    .build();

            // when
            notificationService.push(request);

            // then
            verify(sseEmitterManager).send(USER_ID, request);
        }
    }

    @Nested
    @DisplayName("getUnread")
    class GetUnreadTest {

        @Test
        @DisplayName("미읽음 알림 목록을 반환한다")
        void shouldReturnUnreadNotifications() {
            // given
            Notification notification = createNotification(1L, USER_ID, false);
            given(notificationRepository.findTop100ByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(USER_ID))
                    .willReturn(List.of(notification));

            // when
            NotificationListResponse response = notificationService.getUnread(USER_ID);

            // then
            assertThat(response.notifications()).hasSize(1);
        }

        @Test
        @DisplayName("미읽음 알림이 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenNoUnread() {
            // given
            given(notificationRepository.findTop100ByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(USER_ID))
                    .willReturn(Collections.emptyList());

            // when
            NotificationListResponse response = notificationService.getUnread(USER_ID);

            // then
            assertThat(response.notifications()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAll")
    class GetAllTest {

        @Test
        @DisplayName("전체 알림 목록을 반환한다")
        void shouldReturnAllNotifications() {
            // given
            Notification n1 = createNotification(1L, USER_ID, false);
            Notification n2 = createNotification(2L, USER_ID, true);
            given(notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(USER_ID))
                    .willReturn(List.of(n1, n2));

            // when
            NotificationListResponse response = notificationService.getAll(USER_ID);

            // then
            assertThat(response.notifications()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsReadTest {

        @Test
        @DisplayName("알림 미존재 시 NOTIFICATION_NOT_FOUND 예외를 던진다")
        void shouldThrowWhenNotificationNotFound() {
            // given
            given(notificationRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, 999L))
                    .isInstanceOf(BaseException.class)
                    .satisfies(e -> {
                        BaseException be = (BaseException) e;
                        assertThat(be.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("다른 사용자의 알림이면 NOTIFICATION_FORBIDDEN 예외를 던진다")
        void shouldThrowWhenNotOwner() {
            // given
            Notification notification = createNotification(1L, 999L, false);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(USER_ID, 1L))
                    .isInstanceOf(BaseException.class)
                    .satisfies(e -> {
                        BaseException be = (BaseException) e;
                        assertThat(be.getErrorCode()).isEqualTo(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
                    });
        }

        @Test
        @DisplayName("미읽음 알림을 읽음 처리한다")
        void shouldMarkAsRead() {
            // given
            Notification notification = createNotification(1L, USER_ID, false);
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            // when
            notificationService.markAsRead(USER_ID, 1L);

            // then — 이미 읽음 처리됨 (markAsRead 호출 확인)
            assertThat(notification.getIsRead()).isTrue();
        }
    }

    // ===================== 테스트 픽스처 =====================

    private User createUser(Long userId) {
        try {
            var constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            setField(user, "userId", userId);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("User 생성 실패", e);
        }
    }

    private LoanApplication createLoanApplication(Long applicationId, String productName) {
        try {
            var appConstructor = LoanApplication.class.getDeclaredConstructor();
            appConstructor.setAccessible(true);
            LoanApplication app = appConstructor.newInstance();
            setField(app, "applicationId", applicationId);

            var productConstructor = LoanProduct.class.getDeclaredConstructor();
            productConstructor.setAccessible(true);
            LoanProduct product = productConstructor.newInstance();
            setField(product, "productName", productName);
            setField(app, "product", product);

            return app;
        } catch (Exception e) {
            throw new RuntimeException("LoanApplication 생성 실패", e);
        }
    }

    private Notification createNotification(Long id, Long userId, boolean isRead) {
        try {
            User user = createUser(userId);
            Notification notification = Notification.builder()
                    .user(user)
                    .type(NotificationType.LOAN_DECIDED)
                    .referenceId(10L)
                    .referenceLabel("소상공인 대출")
                    .build();
            setField(notification, "notificationId", id);
            setField(notification, "isRead", isRead);
            setField(notification, "createdAt", LocalDateTime.now());
            return notification;
        } catch (Exception e) {
            throw new RuntimeException("Notification 생성 실패", e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없습니다: " + fieldName);
    }
}
