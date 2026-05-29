package com.sofit.user.domain.notification.service;

import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.notification.enums.NotificationType;
import com.sofit.common.entity.user.User;
import com.sofit.user.domain.notification.dto.response.NotificationListResponse;

public interface NotificationService {

    /** 알림 생성 + DB 저장 + SSE 전송 (sofit-user 내부 발송용) */
    void send(User user, NotificationType type, LoanApplication application);

    /** sofit-admin으로부터 SSE 푸시 수신 (DB 저장 없이 전송만) */
    void push(NotificationPushRequest request);

    /** 미읽음 알림 조회 (최대 100건, 생성일시 내림차순) */
    NotificationListResponse getUnread(Long userId);

    /** 알림 읽음 처리 (소유권 검증 포함) */
    void markAsRead(Long userId, Long notificationId);
}
