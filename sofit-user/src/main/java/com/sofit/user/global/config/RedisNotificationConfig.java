package com.sofit.user.global.config;

import com.sofit.common.dto.notification.NotificationPushRequest;
import com.sofit.user.domain.notification.service.RedisNotificationSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Pub/Sub 알림 설정
 * - 서버 이중화 대응: 모든 인스턴스가 알림 메시지를 수신하여
 *   자신이 보유한 SSE Emitter에만 전송하는 구조
 * - 채널명: notification:push
 */
@Configuration
public class RedisNotificationConfig {

    public static final String NOTIFICATION_CHANNEL = "notification:push";

    /**
     * 알림 Pub/Sub 전용 채널 토픽
     */
    @Bean
    public ChannelTopic notificationTopic() {
        return new ChannelTopic(NOTIFICATION_CHANNEL);
    }

    /**
     * 알림 직렬화/역직렬화 공용 Serializer Bean
     * - JacksonJsonRedisSerializer(Class) 생성자 사용 (Jackson 3 내부 ObjectMapper 자동 생성)
     */
    @Bean
    public JacksonJsonRedisSerializer<NotificationPushRequest> notificationSerializer() {
        return new JacksonJsonRedisSerializer<>(NotificationPushRequest.class);
    }

    /**
     * Redis 메시지 리스너 컨테이너
     * - RedisNotificationSubscriber를 notification:push 채널에 구독 등록
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter notificationListenerAdapter,
            ChannelTopic notificationTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(notificationListenerAdapter, notificationTopic);
        return container;
    }

    /**
     * MessageListenerAdapter: RedisNotificationSubscriber.onMessage()를 호출
     */
    @Bean
    public MessageListenerAdapter notificationListenerAdapter(RedisNotificationSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "onMessage");
    }

    /**
     * 알림 발행 전용 RedisTemplate
     * - Key: String (채널명)
     * - Value: NotificationPushRequest (JacksonJsonRedisSerializer)
     */
    @Bean
    public RedisTemplate<String, NotificationPushRequest> notificationRedisTemplate(
            RedisConnectionFactory connectionFactory,
            JacksonJsonRedisSerializer<NotificationPushRequest> notificationSerializer) {

        RedisTemplate<String, NotificationPushRequest> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(notificationSerializer);
        return template;
    }
}
