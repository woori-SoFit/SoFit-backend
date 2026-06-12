package com.sofit.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * {@link AuditLog} 가 붙은 메서드를 가로채 감사 로그를 자동 적재하는 Aspect.
 *
 * <p>접근기록 필드는 두 곳에서 수집한다.
 * <ul>
 *   <li>MDC: traceId, sourceSystem, clientIp, accessMethod (web 모듈 TraceIdFilter / 배치 스케줄러가 주입)</li>
 *   <li>SecurityContext: actor(로그인 ID), actorRole(권한)</li>
 * </ul>
 * 성공/실패를 모두 기록하며(전자금융감독규정), 감사 기록 실패가 본 비즈니스 로직을
 * 깨뜨리지 않도록 기록은 try-catch 로 격리한다.</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
public class AuditLogAspect {

    private final AuditLogWriter auditLogWriter;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        String result = "SUCCESS";
        try {
            return pjp.proceed();
        } catch (Throwable t) {
            result = "FAILURE";
            throw t;
        } finally {
            try {
                auditLogWriter.write(buildEvent(auditLog, result));
            } catch (Exception ex) {
                // 감사 기록 실패는 비즈니스 흐름을 막지 않는다 (단, 반드시 에러 로그로 남김)
                log.error("[Audit] 감사 로그 기록 실패 action={} result={}", auditLog.action(), result, ex);
            }
        }
    }

    private AuditEvent buildEvent(AuditLog auditLog, String result) {
        String sourceSystem = orDefault(MDC.get("sourceSystem"), "UNKNOWN");
        String accessMethod = orDefault(MDC.get("accessMethod"), "UNKNOWN");
        String traceId = MDC.get("traceId");
        String clientIp = MDC.get("clientIp");

        String actor = sourceSystem;   // 비로그인/배치 기본값
        String actorRole = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(String.valueOf(auth.getPrincipal()))) {
            actor = auth.getName();
            actorRole = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst()
                    .orElse(null);
        }

        String target = auditLog.target().isBlank() ? null : auditLog.target();

        return new AuditEvent(
                LocalDateTime.now(), actor, actorRole, auditLog.action(), target,
                sourceSystem, accessMethod, clientIp, result, traceId);
    }

    private static String orDefault(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }
}
