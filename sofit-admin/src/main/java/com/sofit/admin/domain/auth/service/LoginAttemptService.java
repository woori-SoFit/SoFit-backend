package com.sofit.admin.domain.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 로그인 브루트포스 공격 방어를 위한 시도 횟수 제한 서비스.
 * Redis를 사용하여 계정별 + IP별 로그인 실패 횟수를 추적한다.
 *
 * - IP 기반 제한: 단일 IP에서의 반복 시도를 차단 (계정 DoS 방지)
 * - 계정 기반 제한: 분산 IP 공격에 대비한 계정 보호
 *
 * IP가 먼저 차단되므로, 공격자가 특정 계정의 loginId를 알아도
 * 해당 IP만 차단되고 정상 사용자는 다른 IP에서 로그인 가능하다.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String ACCOUNT_ATTEMPT_PREFIX = "login:attempt:account:";
    private static final String IP_ATTEMPT_PREFIX = "login:attempt:ip:";
    private static final int MAX_ACCOUNT_ATTEMPTS = 10;  // 계정 기준: 분산 IP 공격 대비 (넉넉하게)
    private static final int MAX_IP_ATTEMPTS = 5;        // IP 기준: 단일 IP 브루트포스 차단
    private static final Duration ACCOUNT_LOCK_DURATION = Duration.ofMinutes(30);
    private static final Duration IP_LOCK_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    /**
     * 해당 IP 또는 계정이 잠금 상태인지 확인한다.
     * IP가 먼저 차단되므로 계정 DoS를 방지한다.
     */
    public boolean isBlocked(String loginId, String ipAddress) {
        // IP 기반 차단 우선 확인
        if (isIpBlocked(ipAddress)) {
            return true;
        }
        // 계정 기반 차단 확인 (분산 IP 공격 대비)
        return isAccountBlocked(loginId);
    }

    /**
     * 하위 호환용 — IP 없이 계정만 체크 (테스트 등에서 사용)
     */
    public boolean isBlocked(String loginId) {
        return isAccountBlocked(loginId);
    }

    /**
     * 로그인 실패 시 계정 + IP 모두 시도 횟수를 증가시킨다.
     */
    public void loginFailed(String loginId, String ipAddress) {
        incrementAttempt(ACCOUNT_ATTEMPT_PREFIX + loginId, ACCOUNT_LOCK_DURATION);
        incrementAttempt(IP_ATTEMPT_PREFIX + ipAddress, IP_LOCK_DURATION);
    }

    /**
     * 하위 호환용 — IP 없이 계정만 증가 (테스트 등에서 사용)
     */
    public void loginFailed(String loginId) {
        incrementAttempt(ACCOUNT_ATTEMPT_PREFIX + loginId, ACCOUNT_LOCK_DURATION);
    }

    /**
     * 로그인 성공 시 해당 계정의 시도 횟수를 초기화한다.
     * IP 카운트는 초기화하지 않음 (다른 계정 공격 시도 방지)
     */
    public void loginSucceeded(String loginId) {
        redisTemplate.delete(ACCOUNT_ATTEMPT_PREFIX + loginId);
    }

    private boolean isIpBlocked(String ipAddress) {
        String attempts = redisTemplate.opsForValue().get(IP_ATTEMPT_PREFIX + ipAddress);
        return attempts != null && Integer.parseInt(attempts) >= MAX_IP_ATTEMPTS;
    }

    private boolean isAccountBlocked(String loginId) {
        String attempts = redisTemplate.opsForValue().get(ACCOUNT_ATTEMPT_PREFIX + loginId);
        return attempts != null && Integer.parseInt(attempts) >= MAX_ACCOUNT_ATTEMPTS;
    }

    private void incrementAttempt(String key, Duration ttl) {
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, ttl);
    }
}
