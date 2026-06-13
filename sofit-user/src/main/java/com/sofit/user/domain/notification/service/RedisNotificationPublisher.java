package com.sofit.user.domain.notification.service;

import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.user.global.config.RedisNotificationConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub Publisher
 * - notification:push 채널에 알림 메시지를 발행
 * - 모든 user-backend 인스턴스가 이 메시지를 수신하게 됨
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationPublisher {

    private final RedisTemplate<String, NotificationPushRequest> notificationRedisTemplate;

    /**
     * Redis 채널에 알림 메시지 발행
     */
    public void publish(NotificationPushRequest request) {
        try {
            notificationRedisTemplate.convertAndSend(
                    RedisNotificationConfig.NOTIFICATION_CHANNEL, request);
            log.debug("Redis Pub/Sub 발행 성공: userId={}, type={}",
                    request.getUserId(), request.getType());
        } catch (Exception e) {
            // Redis 장애 시에도 비즈니스 로직에 영향을 주지 않음
            // (DB에 이미 저장된 알림은 미읽음 조회로 복구 가능)
            log.error("Redis Pub/Sub 발행 실패: userId={}, type={}",
                    request.getUserId(), request.getType(), e);
        }
    }
}
