package com.sofit.common.dto.notification;

import com.sofit.common.entity.notification.Notification;
import com.sofit.common.entity.notification.enums.NotificationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPushRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long notificationId;

    @NotNull
    private NotificationType type;

    private String title;

    private String message;

    @NotNull
    private Long referenceId;

    private String referenceLabel;

    private LocalDateTime createdAt;

    private Boolean isRead;

    public static NotificationPushRequest from(Notification notification) {
        return NotificationPushRequest.builder()
                .userId(notification.getUser().getUserId())
                .notificationId(notification.getNotificationId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .referenceId(notification.getReferenceId())
                .referenceLabel(notification.getReferenceLabel())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.getIsRead())
                .build();
    }
}
