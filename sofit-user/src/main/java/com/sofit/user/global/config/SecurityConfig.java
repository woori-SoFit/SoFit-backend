package com.sofit.user.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.user.global.filter.SessionValidationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.ConcurrentSessionControlAuthenticationStrategy;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.security.SpringSessionBackedSessionRegistry;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionValidationFilter sessionValidationFilter;
    private final ObjectMapper objectMapper;
    private final FindByIndexNameSessionRepository<?> sessionRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository())
                        .ignoringRequestMatchers(
                                "/api/auth/signup/**",
                                "/api/auth/login",
                                "/api/notifications/internal/**"
                        )
                )
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                )
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // CSRF 토큰 발급 엔드포인트
                        .requestMatchers("/api/csrf-token").permitAll()
                        // 인증 불필요 경로
                        .requestMatchers("/api/auth/signup/**", "/api/auth/login").permitAll()
                        // 금융인증서 조회 (회원가입 플로우에서도 비인증 상태로 호출)
                        .requestMatchers("/api/financial-cert/lookup").permitAll()
                        // 내부 알림 푸시 API (sofit-admin → sofit-user, 세션 인증 불필요)
                        .requestMatchers("/api/notifications/internal/**").permitAll()
                        // 내 정보 조회는 비로그인 상태에서도 접근 가능 (로그인 여부에 따라 분기)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/users/me").permitAll()
                        // 약관 목록 조회는 비로그인 상태에서 접근 가능 (회원가입 플로우)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/terms/**").permitAll()
                        // 약관 PDF 정적 리소스 비로그인 접근 허용
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/terms/**").permitAll()
                        // 대출 상품 목록/상세 조회는 비로그인 접근 허용
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/loan-products", "/api/loan-products/{productId}").permitAll()
                        // ALB 헬스체크
                        .requestMatchers("/actuator/health").permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().migrateSession()
                        .maximumSessions(1)
                        .sessionRegistry(sessionRegistry())
                        .maxSessionsPreventsLogin(false)
                        .expiredSessionStrategy(event -> {
                            var response = event.getResponse();
                            response.setStatus(GeneralErrorCode.UNAUTHORIZED.getHttpStatus().value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(response.getOutputStream(),
                                    ApiResponse.onFailure(GeneralErrorCode.UNAUTHORIZED));
                        })
                )
                // 절대 만료 체크 필터 등록
                .addFilterBefore(sessionValidationFilter, UsernamePasswordAuthenticationFilter.class)
                // 인증/인가 실패 핸들링
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(GeneralErrorCode.UNAUTHORIZED.getHttpStatus().value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(response.getOutputStream(),
                                    ApiResponse.onFailure(GeneralErrorCode.UNAUTHORIZED));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(GeneralErrorCode.FORBIDDEN.getHttpStatus().value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(response.getOutputStream(),
                                    ApiResponse.onFailure(GeneralErrorCode.FORBIDDEN));
                        })
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:13000",
                "http://localhost:5173",
                "http://172.21.33.214:3000",
                "https://www.sofit.cloud",
                "https://sofit.cloud"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @SuppressWarnings("unchecked")
    public SessionRegistry sessionRegistry() {
        return new SpringSessionBackedSessionRegistry(sessionRepository);
    }

    /**
     * 서비스 레이어에서 호출할 SessionAuthenticationStrategy Bean.
     * 새 로그인 시 기존 세션을 만료시키고 새 로그인을 허용한다.
     * SessionFixation은 SecurityFilterChain의 sessionManagement에서 처리하므로 여기서는 제외.
     */
    @Bean
    public SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        ConcurrentSessionControlAuthenticationStrategy concurrencyStrategy =
                new ConcurrentSessionControlAuthenticationStrategy(sessionRegistry());
        concurrencyStrategy.setMaximumSessions(1);
        concurrencyStrategy.setExceptionIfMaximumExceeded(false);

        RegisterSessionAuthenticationStrategy registerStrategy =
                new RegisterSessionAuthenticationStrategy(sessionRegistry());

        return new CompositeSessionAuthenticationStrategy(
                List.of(concurrencyStrategy, registerStrategy)
        );
    }

    @Bean
    public HttpSessionCsrfTokenRepository csrfTokenRepository() {
        HttpSessionCsrfTokenRepository repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-CSRF-TOKEN");
        return repository;
    }

    @Bean
    public HttpSessionSecurityContextRepository httpSessionSecurityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
