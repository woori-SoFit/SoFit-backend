package com.sofit.user.domain.loan.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("CodefClient 단위 테스트")
class CodefClientTest {

    private CodefClient codefClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        CodefProperties properties = new CodefProperties();
        properties.setClientId("test-client-id");
        properties.setClientSecret("test-client-secret");
        properties.setBaseUrl("http://localhost:9999");
        properties.setOauthUrl("http://localhost:9999/oauth/token");

        codefClient = new CodefClient(properties, redisTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("토큰 캐시 미스 시 토큰 발급 실패하면 ACCOUNT_SERVICE_ERROR 예외를 던진다")
    void shouldThrowWhenTokenRefreshFails() {
        // given — Redis에 캐시 없음 → 토큰 발급 시도 → 외부 서버 접속 불가
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null);

        // when & then
        assertThatThrownBy(() -> codefClient.requestOneWonTransfer("004", "12345678901234"))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> {
                    BaseException be = (BaseException) e;
                    assertThat(be.getErrorCode()).isEqualTo(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
                });
    }

    @Test
    @DisplayName("토큰 캐시 히트 시 API 호출 실패하면 재시도 후 ACCOUNT_SERVICE_ERROR 예외를 던진다")
    void shouldRetryAndThrowWhenApiFails() {
        // given — Redis에 캐시 있음 → API 호출 → 실패 → 토큰 재발급 → 실패
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn("cached-token");

        // when & then
        assertThatThrownBy(() -> codefClient.requestOneWonTransfer("004", "12345678901234"))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> {
                    BaseException be = (BaseException) e;
                    assertThat(be.getErrorCode()).isEqualTo(LoanErrorCode.ACCOUNT_SERVICE_ERROR);
                });
    }
}
