package com.sofit.user.domain.notification.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.notification.Notification;
import com.sofit.common.entity.notification.enums.NotificationType;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.NotificationRepository;
import com.sofit.user.domain.notification.converter.NotificationConverter;
import com.sofit.user.domain.notification.dto.response.NotificationListResponse;
import com.sofit.user.domain.notification.dto.response.NotificationResponse;
import com.sofit.user.domain.notification.exception.NotificationErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterManager sseEmitterManager;

    @Override
    @Transactional
    public void send(User user, NotificationType type, LoanApplication application) {
        // 알림 엔티티 생성 및 DB 저장
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .application(application)
                .build();
        notificationRepository.save(notification);

        // SSE 푸시
        sseEmitterManager.send(user.getUserId(), NotificationPushRequest.from(notification));
    }

    @Override
    public void push(NotificationPushRequest request) {
        sseEmitterManager.send(request.getUserId(), request);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getUnread(Long userId) {
        // 미읽음 알림 최대 100건을 최신순으로 조회
        List<Notification> notifications = notificationRepository
                .findTop100ByUser_UserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        List<NotificationResponse> items = notifications.stream()
                .map(NotificationConverter::toResponse)
                .toList();
        return new NotificationListResponse(items);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        // 알림 존재 여부 확인
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BaseException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        // 소유권 검증
        if (!notification.getUser().getUserId().equals(userId)) {
            throw new BaseException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
        }

        // 이미 읽음이면 read_at 갱신하지 않고 성공 반환
        if (!notification.getIsRead()) {
            notification.markAsRead(LocalDateTime.now());
        }
    }
}
