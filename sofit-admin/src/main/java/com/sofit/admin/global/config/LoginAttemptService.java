package com.sofit.admin.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 로그인 브루트포스 공격 방어를 위한 시도 횟수 제한 서비스.
 * Redis를 사용하여 로그인 실패 횟수를 추적하고, 임계값 초과 시 계정을 일시 잠금한다.
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final String LOGIN_ATTEMPT_PREFIX = "login:attempt:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    /**
     * 해당 loginId가 잠금 상태인지 확인한다.
     */
    public boolean isBlocked(String loginId) {
        String attempts = redisTemplate.opsForValue().get(LOGIN_ATTEMPT_PREFIX + loginId);
        return attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS;
    }

    /**
     * 로그인 실패 시 시도 횟수를 증가시킨다.
     * 최초 실패 시 TTL을 설정하고, 이후 실패 시 카운트만 증가한다.
     */
    public void loginFailed(String loginId) {
        String key = LOGIN_ATTEMPT_PREFIX + loginId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, LOCK_DURATION);
        }
    }

    /**
     * 로그인 성공 시 시도 횟수를 초기화한다.
     */
    public void loginSucceeded(String loginId) {
        redisTemplate.delete(LOGIN_ATTEMPT_PREFIX + loginId);
    }
}
