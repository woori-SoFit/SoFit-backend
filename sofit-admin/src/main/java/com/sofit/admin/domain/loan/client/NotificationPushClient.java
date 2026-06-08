package com.sofit.admin.domain.loan.client;

import com.sofit.common.dto.notification.NotificationPushRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * sofit-user 서버에 알림 SSE 푸시를 요청하는 RestClient 기반 클라이언트.
 * 심사 완료 시 sofit-admin에서 호출하여 사용자에게 실시간 알림을 전달한다.
 *
 * 실패 시 로그만 남기고 예외를 전파하지 않는다.
 * (알림 실패가 심사 처리를 막으면 안 됨 — DB에 이미 알림이 저장되어 있으므로
 *  사용자가 앱 재진입 시 미읽음 조회로 확인 가능)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushClient {

    private final RestClient notificationRestClient;

    public void pushNotification(NotificationPushRequest request) {
        try {
            notificationRestClient.post()
                    .uri("/api/notifications/internal/push")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("알림 푸시 실패: userId={}, type={}", request.getUserId(), request.getType(), e);
        }
    }
}
