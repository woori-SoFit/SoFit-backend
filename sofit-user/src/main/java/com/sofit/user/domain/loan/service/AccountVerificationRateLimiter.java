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
 * Redis INCR + TTL(자정까지 남은 초) 패턴 사용
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
     * 해당 계좌번호의 당일 요청이 허용되는지 확인
     *
     * @param accountNumber 계좌번호
     * @return true면 허용, false면 한도 초과
     * @throws BaseException ACCOUNT5001 - Redis 연결 실패 시
     */
    public boolean isAllowed(String accountNumber) {
        try {
            String key = buildKey(accountNumber);
            String value = redisTemplate.opsForValue().get(key);

            if (value == null) {
                return true;
            }

            return Integer.parseInt(value) < MAX_REQUESTS_PER_DAY;
        } catch (Exception e) {
            log.error("Rate Limiter Redis 조회 실패: {}", e.getMessage());
            throw new BaseException(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
        }
    }

    /**
     * 해당 계좌번호의 당일 요청 횟수를 1 증가
     * 키가 처음 생성되면 자정까지 남은 초를 TTL로 설정
     *
     * @param accountNumber 계좌번호
     * @throws BaseException ACCOUNT5001 - Redis 연결 실패 시
     */
    public void increment(String accountNumber) {
        try {
            String key = buildKey(accountNumber);
            Long count = redisTemplate.opsForValue().increment(key);

            // 첫 번째 요청이면 TTL 설정 (자정까지 남은 초)
            if (count != null && count == 1L) {
                long secondsUntilMidnight = getSecondsUntilMidnight();
                redisTemplate.expire(key, secondsUntilMidnight, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.error("Rate Limiter Redis 증가 실패: {}", e.getMessage());
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
