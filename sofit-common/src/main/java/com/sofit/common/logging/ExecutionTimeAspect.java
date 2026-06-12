package com.sofit.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * controller / service 공통 실행시간 로깅 (횡단 관심사).
 *
 * <p><b>레벨 정책(설계 §3-4)</b>: 정상 흐름은 DEBUG(운영 OFF)로만 남겨 INFO 폭주를 막고,
 * {@code elapsed >= SLOW_MS} 인 느린 구간만 WARN 으로 끌어올린다. 예외는 ERROR.
 * AWS 백엔드 ↔ 온프렘 DB 원격 구간 지연을 메서드 단위로 분리 추적하는 용도.</p>
 *
 * <p>인자값({@code pjp.getArgs()})은 민감정보(주민번호·계좌·고객명 등) 유출 위험이 있어
 * <b>절대 로깅하지 않는다</b>. 메서드 시그니처와 소요시간만 남긴다.</p>
 *
 * <p>self-invocation(같은 빈 내부 호출)은 프록시를 거치지 않아 측정되지 않는다(정상).
 * 필요 시 {@code logging.execution-aspect.enabled=false} 로 끌 수 있다.</p>
 */
@Slf4j
@Aspect
@Component
@ConditionalOnProperty(name = "logging.execution-aspect.enabled", havingValue = "true", matchIfMissing = true)
public class ExecutionTimeAspect {

    /** 이 시간(ms) 이상 걸리면 WARN 으로 승격 */
    private static final long SLOW_MS = 1000;

    @Around("execution(* com.sofit..controller..*(..)) || execution(* com.sofit..service..*(..))")
    public Object logExecution(ProceedingJoinPoint pjp) throws Throwable {
        long start = System.currentTimeMillis();
        String signature = pjp.getSignature().toShortString();
        try {
            Object result = pjp.proceed();
            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= SLOW_MS) {
                log.warn("[slow] {} {}ms", signature, elapsed);
            } else if (log.isDebugEnabled()) {
                log.debug("{} {}ms", signature, elapsed);
            }
            return result;
        } catch (Throwable t) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("{} 실패 {}ms: {}", signature, elapsed, t.toString());
            throw t;
        }
    }
}
