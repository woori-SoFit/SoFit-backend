package com.sofit.admin.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisIndexedHttpSession;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableRedisIndexedHttpSession
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final FindByIndexNameSessionRepository<?> sessionRepository;

    @Value("${app.cors.allowed-origins:http://localhost:3001}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .ignoringRequestMatchers("/api/admin/auth/login")
                )
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // CSRF 토큰 발급 엔드포인트
                        .requestMatchers("/api/admin/csrf-token").permitAll()
                        // 로그인 엔드포인트 허용
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        // Prometheus 모니터링 (monitor 인스턴스만 접근, SG로 제어)
                        .requestMatchers("/actuator/prometheus", "/actuator/health").permitAll()
                        // 세분화된 역할 규칙 (구체적 경로 우선)
                        .requestMatchers("/api/admin/loan-applications/*/approve", "/api/admin/loan-applications/*/reject")
                            .hasAnyAuthority("ADMIN_BANK_TELLER", "ADMIN_BANK_MANAGER")
                        .requestMatchers("/api/admin/dev/**").hasAuthority("ADMIN_DEV")
                        // 고객 정보 목록 조회: 모든 관리자 역할 허용
                        .requestMatchers("/api/admin/users/**").hasAnyAuthority("ADMIN_DEV", "ADMIN_BANK_TELLER", "ADMIN_BANK_MANAGER")
                        // 나머지 admin 경로: 모든 관리자 역할 허용
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN_BANK_TELLER", "ADMIN_BANK_MANAGER", "ADMIN_DEV")
                        // 정의되지 않은 경로는 전면 차단
                        .anyRequest().denyAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().newSession()
                        .maximumSessions(1)
                        .sessionRegistry(sessionRegistry())
                        .maxSessionsPreventsLogin(true));
        return http.build();
    }

    @Bean
    @SuppressWarnings("unchecked")
    public SessionRegistry sessionRegistry() {
        return new SpringSessionBackedSessionRegistry(sessionRepository);
    }

    /**
     * 서비스 레이어에서 호출할 SessionAuthenticationStrategy Bean.
     * Spring Security의 동시 세션 제어를 단일 지점에서 처리한다.
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        ConcurrentSessionControlAuthenticationStrategy concurrencyStrategy =
                new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry());
        concurrencyStrategy.setMaximumSessions(1);
        concurrencyStrategy.setExceptionIfMaximumExceeded(true);

        SessionFixationProtectionStrategy fixationStrategy = new SessionFixationProtectionStrategy();

        RegisterSessionAuthenticationStrategy registerStrategy =
                new RegisterSessionAuthenticationStrategy(sessionRegistry());

        return new CompositeSessionAuthenticationStrategy(
                List.of(concurrencyStrategy, fixationStrategy, registerStrategy)
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:13001",
                "http://localhost:5173",
                "http://172.21.33.214:3001"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public HttpSessionCsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }
}
