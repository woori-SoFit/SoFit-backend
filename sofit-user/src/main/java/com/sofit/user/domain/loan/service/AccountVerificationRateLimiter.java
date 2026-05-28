package com.sofit.user.domain.loan.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.loan.exception.LoanErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 계좌 인증 일일 요청 횟수 제한 (계좌번호당 5회/일)
 * Redis INCR 원자적 연산으로 허용/거부를 한 번에 판정
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccountVerificationRateLimiter {

    private static final String KEY_PREFIX = "account:rate:";
    private static final int MAX_REQUESTS_PER_DAY = 5;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redisTemplate;

    /**
     * 요청 횟수를 원자적으로 증가시키고 허용 여부를 판정한다.
     * INCR은 원자적 연산이므로 race condition이 발생하지 않는다.
     *
     * @param accountNumber 계좌번호
     * @throws BaseException ACCOUNT4002 - 한도 초과 시
     * @throws BaseException ACCOUNT5001 - Redis 연결 실패 시
     */
    public void checkAndIncrement(String accountNumber) {
        try {
            String key = buildKey(accountNumber);
            Long count = redisTemplate.opsForValue().increment(key);

            // 매 요청마다 TTL 설정 (INCR 후 TTL이 없는 경우 방지)
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (ttl == null || ttl < 0) {
                long secondsUntilMidnight = getSecondsUntilMidnight();
                redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
            }

            // 한도 초과 시 예외
            if (count != null && count > MAX_REQUESTS_PER_DAY) {
                throw new BaseException(LoanErrorCode.ACCOUNT_RATE_LIMIT_EXCEEDED);
            }
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Rate Limiter Redis 연산 실패: {}", e.getMessage());
            throw new BaseException(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
        }
    }

    /**
     * Redis 키 생성: account:rate:{accountNumber}:{yyyyMMdd}
     */
    private String buildKey(String accountNumber) {
        String today = LocalDate.now(KST).format(DATE_FORMAT);
        return KEY_PREFIX + accountNumber + ":" + today;
    }

    /**
     * KST 기준 자정까지 남은 초 계산
     */
    private long getSecondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now(KST);
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return ChronoUnit.SECONDS.between(now, midnight);
    }
}
