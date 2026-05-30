package com.sofit.user.domain.notification.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.user.domain.notification.dto.response.NotificationListResponse;
import com.sofit.user.domain.notification.exception.NotificationSuccessCode;
import com.sofit.user.domain.notification.service.NotificationService;
import com.sofit.user.domain.notification.service.SseEmitterManager;
import com.sofit.user.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

    private final SseEmitterManager sseEmitterManager;
    private final NotificationService notificationService;

    @Override
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Long userId = SecurityUtil.getCurrentUserId();
        return sseEmitterManager.subscribe(userId);
    }

    @Override
    @GetMapping("/unread")
    public ApiResponse<NotificationListResponse> getUnread() {
        Long userId = SecurityUtil.getCurrentUserId();
        NotificationListResponse response = notificationService.getUnread(userId);
        return ApiResponse.onSuccess(NotificationSuccessCode.UNREAD_LIST_OK, response);
    }

    @Override
    @GetMapping
    public ApiResponse<NotificationListResponse> getAll() {
        Long userId = SecurityUtil.getCurrentUserId();
        NotificationListResponse response = notificationService.getAll(userId);
        return ApiResponse.onSuccess(NotificationSuccessCode.NOTIFICATION_LIST_OK, response);
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(@PathVariable Long notificationId) {
        Long userId = SecurityUtil.getCurrentUserId();
        notificationService.markAsRead(userId, notificationId);
        return ApiResponse.onSuccess(NotificationSuccessCode.MARK_READ_OK, null);
    }

    @Override
    @PostMapping("/internal/push")
    public ApiResponse<Void> push(@Valid @RequestBody NotificationPushRequest request) {
        notificationService.push(request);
        return ApiResponse.onSuccess(NotificationSuccessCode.PUSH_OK, null);
    }
}
