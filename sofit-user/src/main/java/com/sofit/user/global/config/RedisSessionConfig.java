package com.sofit.user.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Redis 세션 설정
 * - 유휴 만료: 30분 (maxInactiveIntervalInSeconds = 1800)
 * - 절대 만료: 12시간 (SessionValidationFilter에서 처리)
 * - FindByIndexNameSessionRepository를 통해 userId로 세션 역조회 가능
 */
@Configuration
@EnableRedisIndexedHttpSession(maxInactiveIntervalInSeconds = 1800)
public class RedisSessionConfig {

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("SESSION");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(false); // TODO: 운영 환경에서는 true로 변경
        serializer.setSameSite("Lax");
        serializer.setCookiePath("/");
        return serializer;
    }
}
