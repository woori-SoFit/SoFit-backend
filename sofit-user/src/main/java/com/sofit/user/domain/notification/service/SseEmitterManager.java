package com.sofit.user.domain.notification.service;

import com.sofit.common.dto.notification.NotificationPushRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SseEmitterManager {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 30 * 60 * 1000L; // 30분

    /**
     * SSE 구독: userId에 대한 SseEmitter를 생성하고 Map에 저장.
     * 동일 userId로 재구독 시 기존 emitter를 덮어쓴다.
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        // 연결 직후 더미 이벤트 전송 (503 방지)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId);
            log.warn("SSE 더미 이벤트 전송 실패: userId={}", userId, e);
        }

        return emitter;
    }

    /**
     * SSE 이벤트 전송: 해당 userId의 emitter가 있으면 전송, 없으면(오프라인) 무시.
     * 전송 실패 시 emitter를 제거하고 예외를 전파하지 않는다.
     */
    public void send(Long userId, NotificationPushRequest payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event().name("notification").data(payload));
        } catch (IOException e) {
            emitters.remove(userId);
            log.warn("SSE 이벤트 전송 실패: userId={}", userId, e);
        }
    }
}
