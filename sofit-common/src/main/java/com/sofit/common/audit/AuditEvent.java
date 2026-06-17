package com.sofit.common.audit;

import java.time.LocalDateTime;

/**
 * 감사 로그 1건. 전자금융감독규정 접근기록 필드(누가/언제/무엇을/어떻게/결과)를 담는다.
 * 무결성 컬럼(hmac, prevHash)은 다음 이터레이션에서 추가 — 현재는 null 로 적재.
 */
public record AuditEvent(
        LocalDateTime eventTime,   // 언제 (NTP 동기화 전제)
        String actor,              // 누가 (로그인 사용자 / BATCH / SYSTEM)
        String actorRole,          // 권한
        String action,             // 무엇을
        String target,             // 대상
        String sourceSystem,       // 어느 서버 (USER / ADMIN / BATCH)
        String accessMethod,       // 어떻게 (WEB / BATCH)
        String clientIp,           // 접근 IP
        String result,             // SUCCESS / FAILURE (실패도 반드시 기록)
        String traceId             // 추적 ID (MDC traceId 와 동일)
) {
}
