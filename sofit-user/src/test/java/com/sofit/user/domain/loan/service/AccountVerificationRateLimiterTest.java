package com.sofit.user.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.loan.exception.LoanErrorCode;

@ExtendWith(MockitoExtension.class)
class AccountVerificationRateLimiterTest {

    @InjectMocks
    private AccountVerificationRateLimiter rateLimiter;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private static final String ACCOUNT_NUMBER = "1234567890";

    @Test
    @DisplayName("한도 이내(count <= 5)면 예외 없이 통과한다")
    void checkAndIncrement_underLimit_passes() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(3L);
        given(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).willReturn(100L);

        // when & then
        assertThatCode(() -> rateLimiter.checkAndIncrement(ACCOUNT_NUMBER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("경계값(count == 5)이면 통과한다")
    void checkAndIncrement_atLimit_passes() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(5L);
        given(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).willReturn(100L);

        // when & then
        assertThatCode(() -> rateLimiter.checkAndIncrement(ACCOUNT_NUMBER))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("한도 초과(count == 6)면 ACCOUNT_RATE_LIMIT_EXCEEDED 예외를 던진다")
    void checkAndIncrement_overLimit_throwsRateLimitExceeded() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(6L);
        given(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).willReturn(100L);

        // when & then
        assertThatThrownBy(() -> rateLimiter.checkAndIncrement(ACCOUNT_NUMBER))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.ACCOUNT_RATE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("TTL이 없으면(null) 자정까지 남은 시간으로 만료시간을 설정한다")
    void checkAndIncrement_noTtl_setsExpire() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(1L);
        given(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).willReturn(null);

        // when
        rateLimiter.checkAndIncrement(ACCOUNT_NUMBER);

        // then
        verify(redisTemplate).expire(anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("TTL이 이미 설정돼 있으면(양수) 만료시간을 다시 설정하지 않는다")
    void checkAndIncrement_existingTtl_doesNotResetExpire() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString())).willReturn(2L);
        given(redisTemplate.getExpire(anyString(), eq(TimeUnit.SECONDS))).willReturn(3600L);

        // when
        rateLimiter.checkAndIncrement(ACCOUNT_NUMBER);

        // then
        verify(redisTemplate, never()).expire(anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("Redis 연산 실패 시 ACCOUNT_SERVICE_ERROR 예외로 변환한다")
    void checkAndIncrement_redisFailure_throwsServiceError() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment(anyString()))
                .willThrow(new RuntimeException("Redis connection failed"));

        // when & then
        assertThatThrownBy(() -> rateLimiter.checkAndIncrement(ACCOUNT_NUMBER))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
    }
}
