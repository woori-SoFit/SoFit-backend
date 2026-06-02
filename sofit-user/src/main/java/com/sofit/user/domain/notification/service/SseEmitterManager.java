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
     * 동일 userId로 재구독 시 기존 emitter를 complete 처리한 뒤 새 emitter로 교체한다.
     */
    public SseEmitter subscribe(Long userId) {
        // 1. 기존 emitter가 있으면 complete 처리 (정리)
        SseEmitter oldEmitter = emitters.get(userId);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        // 2. 새 SseEmitter 생성 (30분 타임아웃)
        SseEmitter emitter = new SseEmitter(TIMEOUT);

        // 3. Map에 저장
        emitters.put(userId, emitter);

        // 4. 연결 종료 시 Map에서 제거 (값 비교 포함 — 자신만 제거)
        emitter.onCompletion(() -> emitters.remove(userId, emitter));
        emitter.onTimeout(() -> emitters.remove(userId, emitter));
        emitter.onError(e -> emitters.remove(userId, emitter));

        // 5. 연결 직후 더미 이벤트 전송 (503 방지)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId, emitter);
            log.warn("SSE 더미 이벤트 전송 실패: userId={}", userId, e);
        }

        return emitter;
    }

    /**
     * SSE 이벤트 전송: 해당 userId의 emitter가 있으면 전송, 없으면(오프라인) 무시.
     * 전송 실패 시 해당 emitter를 제거하고 예외를 전파하지 않는다.
     */
    public void send(Long userId, NotificationPushRequest payload) {
        // 1. Map에서 해당 userId의 emitter 조회
        SseEmitter emitter = emitters.get(userId);

        // 2. null이면 오프라인 상태
        if (emitter == null) {
            return;
        }

        // 3. SSE 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                .name("notification")
                .data(payload));
        } catch (IOException e) {
            // 4. 전송 실패 시 해당 emitter만 제거 (값 비교)
            emitters.remove(userId, emitter);
            log.warn("SSE 이벤트 전송 실패: userId={}", userId, e);
        }
    }
}
