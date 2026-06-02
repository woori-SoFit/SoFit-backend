package com.sofit.user.domain.notification.dto.response;

import com.sofit.common.entity.notification.enums.NotificationType;

import java.time.LocalDateTime;

public record NotificationResponse(
    Long notificationId,
    NotificationType type,
    String title,
    String message,
    Long applicationId,
    Boolean isRead,
    LocalDateTime createdAt
) {}
