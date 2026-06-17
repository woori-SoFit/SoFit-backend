package com.sofit.user.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Redis 세션 설정
 * - 유휴 만료: 10분 (maxInactiveIntervalInSeconds = 600)
 * - 절대 만료: 12시간 (SessionValidationFilter에서 처리)
 * - FindByIndexNameSessionRepository를 통해 userId로 세션 역조회 가능
 */
@Configuration
@EnableRedisIndexedHttpSession(maxInactiveIntervalInSeconds = 600)
public class RedisSessionConfig {

    /**
     * ElastiCache(관리형 Redis)는 클라이언트의 CONFIG SET 명령을 차단한다.
     * 기본 ConfigureNotifyKeyspaceEventsAction이 시작 시 CONFIG SET을 호출해
     * 앱 기동이 실패하므로 NO_OP으로 비활성화.
     * (keyspace notification은 ElastiCache 파라미터 그룹에서 notify-keyspace-events로 설정)
     */
    @Bean
    public ConfigureRedisAction configureRedisAction() {
        return ConfigureRedisAction.NO_OP;
    }

    @Bean
    @Profile("release")
    public CookieSerializer releaseCookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(true);
        serializer.setSameSite("Lax");
        serializer.setCookiePath("/");
        return serializer;
    }

    @Bean
    @Profile({"local", "dev", "test"})
    public CookieSerializer devCookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(false);
        serializer.setSameSite("Lax");
        serializer.setCookiePath("/");
        return serializer;
    }
}
