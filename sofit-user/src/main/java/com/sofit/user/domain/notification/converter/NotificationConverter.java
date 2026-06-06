package com.sofit.user.domain.notification.converter;

import com.sofit.common.entity.notification.Notification;
import com.sofit.user.domain.notification.dto.response.NotificationResponse;

public class NotificationConverter {

    private NotificationConverter() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getNotificationId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getReferenceId(),
                notification.getReferenceLabel(),
                notification.getIsRead(),
                notification.getCreatedAt()
        );
    }
}
