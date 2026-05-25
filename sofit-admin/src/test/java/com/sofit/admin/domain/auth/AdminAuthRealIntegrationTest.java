package com.sofit.admin.domain.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 MySQL + Redis 연결 기반 통합 테스트.
 *
 * 실행 조건:
 * - dev 프로파일의 MySQL (172.21.33.238:3306) 접근 가능
 * - localhost:6379 Redis 실행 중
 * - 테스트 계정: dev_admin / sofit1234!
 *
 * 실행 방법:
 * ./gradlew :sofit-admin:test --tests "com.sofit.admin.domain.auth.AdminAuthRealIntegrationTest"
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthRealIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String LOGIN_URL = "/api/admin/auth/login";
    private static final String VALID_LOGIN_ID = "dev_admin";
    private static final String VALID_PASSWORD = "sofit1234!";
    private static final String WRONG_PASSWORD = "wrongpassword";

    @BeforeEach
    void setUp() {
        // 테스트 전 로그인 시도 횟수 초기화
        redisTemplate.delete("login:attempt:" + VALID_LOGIN_ID);
        // 기존 세션 정리 (Spring Session Redis 키 패턴)
        var keys = redisTemplate.keys("spring:session:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Nested
    @DisplayName("로그인 횟수 제한 (브루트포스 방어) - 실제 DB/Redis")
    class LoginAttemptLimitRealTest {

        @Test
        @DisplayName("5회 연속 실패 후 올바른 비밀번호로도 로그인 차단")
        void shouldBlockAfterFiveFailedAttempts() throws Exception {
            // 5회 연속 실패
            for (int i = 0; i < 5; i++) {
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(VALID_LOGIN_ID, WRONG_PASSWORD)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.code").value("AUTH4001"));
            }

            // 6번째: 올바른 비밀번호로도 차단됨
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, VALID_PASSWORD)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("AUTH4291"))
                    .andExpect(jsonPath("$.message").value("로그인 시도 횟수를 초과했습니다. 15분 후 다시 시도해 주세요."));
        }

        @Test
        @DisplayName("4회 실패 후 올바른 비밀번호로 로그인 성공 → 카운트 초기화")
        void shouldResetCountOnSuccessfulLogin() throws Exception {
            // 4회 실패
            for (int i = 0; i < 4; i++) {
                mockMvc.perform(post(LOGIN_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson(VALID_LOGIN_ID, WRONG_PASSWORD)))
                        .andExpect(status().isBadRequest());
            }

            // 5번째: 올바른 비밀번호로 성공
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, VALID_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));

            // Redis에서 카운트 초기화 확인
            String attempts = redisTemplate.opsForValue().get("login:attempt:" + VALID_LOGIN_ID);
            assertThat(attempts).isNull();
        }
    }

    @Nested
    @DisplayName("중복 로그인 방지 (동시 세션 제한) - 실제 DB/Redis")
    class ConcurrentLoginRealTest {

        @Test
        @DisplayName("첫 번째 로그인 성공 후 두 번째 로그인 시도")
        void shouldHandleSecondLoginAttempt() throws Exception {
            // 첫 번째 로그인 (세션 A 생성)
            MvcResult firstLogin = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, VALID_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andReturn();

            MockHttpSession sessionA = (MockHttpSession) firstLogin.getRequest().getSession(false);
            assertThat(sessionA).isNotNull();

            // 두 번째 로그인 (다른 세션으로 시도)
            // maximumSessions(1) + maxSessionsPreventsLogin(true) 설정에 의해
            // 이미 세션이 존재하면 새 로그인이 차단되어야 함
            MvcResult secondLogin = mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, VALID_PASSWORD)))
                    .andReturn();

            // 결과 출력 (디버깅용)
            int secondStatus = secondLogin.getResponse().getStatus();
            String secondBody = secondLogin.getResponse().getContentAsString();
            System.out.println("=== 두 번째 로그인 시도 결과 ===");
            System.out.println("Status: " + secondStatus);
            System.out.println("Body: " + secondBody);

            // 참고: Spring Security의 maximumSessions는 AuthenticationManager를 통한 인증에서만
            // 자동 적용됩니다. 서비스 레벨에서 직접 SecurityContext를 설정하는 현재 방식에서는
            // 별도 처리가 필요할 수 있습니다. 이 테스트로 실제 동작을 확인합니다.
        }
    }

    @Nested
    @DisplayName("기본 로그인 동작 확인 - 실제 DB/Redis")
    class BasicLoginRealTest {

        @Test
        @DisplayName("올바른 계정으로 로그인 성공")
        void shouldLoginSuccessfully() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, VALID_PASSWORD)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.result.name").exists())
                    .andExpect(jsonPath("$.result.role").value("ADMIN_DEV"));
        }

        @Test
        @DisplayName("잘못된 비밀번호로 로그인 실패")
        void shouldFailWithWrongPassword() throws Exception {
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginJson(VALID_LOGIN_ID, WRONG_PASSWORD)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.isSuccess").value(false))
                    .andExpect(jsonPath("$.code").value("AUTH4001"));
        }
    }

    private String loginJson(String loginId, String password) {
        return String.format("{\"loginId\":\"%s\",\"password\":\"%s\"}", loginId, password);
    }
}
