package com.sofit.user.domain.notification.service;

import com.sofit.common.dto.notification.NotificationPushRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub Subscriber
 * - notification:push 채널 메시지를 수신하여 로컬 SseEmitterManager로 전달
 * - 해당 userId의 emitter가 이 인스턴스에 있으면 SSE 전송, 없으면 무시
 * - Config에서 등록한 notificationSerializer Bean을 주입받아
 *   Publisher와 동일한 ObjectMapper 설정으로 역직렬화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisNotificationSubscriber implements MessageListener {

    private final SseEmitterManager sseEmitterManager;
    private final JacksonJsonRedisSerializer<NotificationPushRequest> notificationSerializer;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            NotificationPushRequest request = notificationSerializer.deserialize(message.getBody());

            log.debug("Redis Pub/Sub 수신: userId={}, type={}",
                    request.getUserId(), request.getType());

            // 로컬 인스턴스의 emitter에 전송 시도 (없으면 SseEmitterManager 내부에서 무시)
            sseEmitterManager.send(request.getUserId(), request);
        } catch (Exception e) {
            log.error("Redis Pub/Sub 메시지 역직렬화 실패", e);
        }
    }
}
