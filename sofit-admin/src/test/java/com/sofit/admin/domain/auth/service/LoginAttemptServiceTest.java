package com.sofit.admin.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    @InjectMocks
    private LoginAttemptService loginAttemptService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    // ===== isBlocked(loginId, ipAddress) 테스트 =====

    @Nested
    @DisplayName("isBlocked(loginId, ipAddress)")
    class IsBlockedWithIpTest {

        @Test
        @DisplayName("IP 시도 횟수가 5 이상이면 차단 상태를 반환한다")
        void isBlocked_IP_시도횟수_5이상이면_차단() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("login:attempt:ip:127.0.0.1")).willReturn("5");

            // when
            boolean result = loginAttemptService.isBlocked("testuser", "127.0.0.1");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("계정 시도 횟수가 10 이상이면 차단 상태를 반환한다")
        void isBlocked_계정_시도횟수_10이상이면_차단() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("login:attempt:ip:127.0.0.1")).willReturn("3");
            given(valueOperations.get("login:attempt:account:testuser")).willReturn("10");

            // when
            boolean result = loginAttemptService.isBlocked("testuser", "127.0.0.1");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("IP와 계정 시도 횟수가 모두 임계값 미만이면 차단하지 않는다")
        void isBlocked_임계값_미만이면_차단_안함() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("login:attempt:ip:127.0.0.1")).willReturn("4");
            given(valueOperations.get("login:attempt:account:testuser")).willReturn("9");

            // when
            boolean result = loginAttemptService.isBlocked("testuser", "127.0.0.1");

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Redis 값이 null이면 차단하지 않는다")
        void isBlocked_Redis값_null이면_차단_안함() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("login:attempt:ip:127.0.0.1")).willReturn(null);
            given(valueOperations.get("login:attempt:account:testuser")).willReturn(null);

            // when
            boolean result = loginAttemptService.isBlocked("testuser", "127.0.0.1");

            // then
            assertThat(result).isFalse();
        }
    }

    // ===== isBlocked(loginId) 테스트 (하위 호환용) =====

    @Nested
    @DisplayName("isBlocked(loginId)")
    class IsBlockedAccountOnlyTest {

        @Test
        @DisplayName("계정 시도 횟수가 10 이상이면 차단 상태를 반환한다")
        void isBlocked_계정_시도횟수_10이상이면_차단() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("login:attempt:account:testuser")).willReturn("10");

            // when
            boolean result = loginAttemptService.isBlocked("testuser");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("계정 시도 횟수가 9이면 차단하지 않는다")
        void isBlocked_계정_시도횟수_9이면_차단_안함() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.get("login:attempt:account:testuser")).willReturn("9");

            // when
            boolean result = loginAttemptService.isBlocked("testuser");

            // then
            assertThat(result).isFalse();
        }
    }

    // ===== loginFailed(loginId, ipAddress) 테스트 =====

    @Nested
    @DisplayName("loginFailed(loginId, ipAddress)")
    class LoginFailedTest {

        @Test
        @DisplayName("로그인 실패 시 계정과 IP 시도 횟수를 모두 증가시킨다")
        void loginFailed_계정과_IP_시도횟수_모두_증가() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.increment("login:attempt:account:testuser")).willReturn(1L);
            given(valueOperations.increment("login:attempt:ip:127.0.0.1")).willReturn(1L);

            // when
            loginAttemptService.loginFailed("testuser", "127.0.0.1");

            // then
            verify(valueOperations).increment("login:attempt:account:testuser");
            verify(valueOperations).increment("login:attempt:ip:127.0.0.1");
        }

        @Test
        @DisplayName("첫 번째 실패(count=1)이면 TTL을 설정한다")
        void loginFailed_첫번째_실패이면_TTL_설정() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.increment("login:attempt:account:testuser")).willReturn(1L);
            given(valueOperations.increment("login:attempt:ip:127.0.0.1")).willReturn(1L);

            // when
            loginAttemptService.loginFailed("testuser", "127.0.0.1");

            // then
            verify(redisTemplate).expire(eq("login:attempt:account:testuser"), any());
            verify(redisTemplate).expire(eq("login:attempt:ip:127.0.0.1"), any());
        }

        @Test
        @DisplayName("두 번째 이상 실패이면 TTL을 재설정하지 않는다")
        void loginFailed_두번째_이상_실패이면_TTL_재설정_안함() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.increment("login:attempt:account:testuser")).willReturn(2L);
            given(valueOperations.increment("login:attempt:ip:127.0.0.1")).willReturn(2L);

            // when
            loginAttemptService.loginFailed("testuser", "127.0.0.1");

            // then - expire 호출 없음
            verify(redisTemplate, org.mockito.Mockito.never())
                    .expire(eq("login:attempt:account:testuser"), any());
            verify(redisTemplate, org.mockito.Mockito.never())
                    .expire(eq("login:attempt:ip:127.0.0.1"), any());
        }
    }

    // ===== loginFailed(loginId) 테스트 (하위 호환용) =====

    @Nested
    @DisplayName("loginFailed(loginId)")
    class LoginFailedAccountOnlyTest {

        @Test
        @DisplayName("IP 없이 계정 시도 횟수만 증가시킨다")
        void loginFailed_계정_시도횟수만_증가() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.increment("login:attempt:account:testuser")).willReturn(1L);

            // when
            loginAttemptService.loginFailed("testuser");

            // then
            verify(valueOperations).increment("login:attempt:account:testuser");
            verify(redisTemplate).expire(eq("login:attempt:account:testuser"), any());
        }

        @Test
        @DisplayName("두 번째 이상 실패이면 TTL을 재설정하지 않는다")
        void loginFailed_두번째_이상_실패이면_TTL_재설정_안함() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.increment("login:attempt:account:testuser")).willReturn(3L);

            // when
            loginAttemptService.loginFailed("testuser");

            // then
            verify(redisTemplate, org.mockito.Mockito.never())
                    .expire(eq("login:attempt:account:testuser"), any());
        }
    }

    // ===== incrementAttempt null 케이스 =====

    @Nested
    @DisplayName("incrementAttempt - count null")
    class IncrementAttemptNullTest {

        @Test
        @DisplayName("increment 결과가 null이면 expire를 호출하지 않는다")
        void incrementAttempt_count_null이면_expire_안함() {
            // given
            given(redisTemplate.opsForValue()).willReturn(valueOperations);
            given(valueOperations.increment("login:attempt:account:testuser")).willReturn(null);
            given(valueOperations.increment("login:attempt:ip:127.0.0.1")).willReturn(null);

            // when
            loginAttemptService.loginFailed("testuser", "127.0.0.1");

            // then - expire 호출 없음
            verify(redisTemplate, org.mockito.Mockito.never())
                    .expire(eq("login:attempt:account:testuser"), any());
            verify(redisTemplate, org.mockito.Mockito.never())
                    .expire(eq("login:attempt:ip:127.0.0.1"), any());
        }
    }

    // ===== loginSucceeded 테스트 =====

    @Nested
    @DisplayName("loginSucceeded")
    class LoginSucceededTest {

        @Test
        @DisplayName("로그인 성공 시 계정 시도 횟수 키를 삭제한다")
        void loginSucceeded_계정_시도횟수_키_삭제() {
            // when
            loginAttemptService.loginSucceeded("testuser");

            // then
            verify(redisTemplate).delete("login:attempt:account:testuser");
        }
    }
}
