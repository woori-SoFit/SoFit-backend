package com.sofit.user.domain.notification.service;

import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.common.entity.notification.enums.NotificationType;
import com.sofit.user.domain.notification.dto.response.NotificationListResponse;

public interface NotificationService {

    // 알림 생성 + DB 저장 + SSE 전송 (sofit-user 내부 발송용)
    void send(Long userId, NotificationType type, Long applicationId);

    // sofit-admin으로부터 SSE 푸시 수신 (DB 저장 없이 전송만)
    void push(NotificationPushRequest request);

    // 미읽음 알림 조회
    NotificationListResponse getUnread(Long userId);

    // 전체 알림 목록 조회 (읽음/미읽음 모두 포함, 최신순)
    NotificationListResponse getAll(Long userId);

    // 알림 읽음 처리
    void markAsRead(Long userId, Long notificationId);
}
