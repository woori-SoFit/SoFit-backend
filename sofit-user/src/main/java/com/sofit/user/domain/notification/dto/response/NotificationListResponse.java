package com.sofit.user.domain.notification.dto.response;

import java.util.List;

public record NotificationListResponse(
    List<NotificationResponse> notifications
) {}
