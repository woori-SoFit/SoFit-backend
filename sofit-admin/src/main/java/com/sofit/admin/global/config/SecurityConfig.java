package com.sofit.admin.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 로그인 엔드포인트 허용
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        // 세분화된 역할 규칙 (구체적 경로 우선)
                        .requestMatchers("/api/admin/manager/loan-applications/*/approve").hasAuthority("ADMIN_BANK_MANAGER")
                        .requestMatchers("/api/admin/dev/**").hasAuthority("ADMIN_DEV")
                        // 나머지 admin 경로: 모든 관리자 역할 허용
                        .requestMatchers("/api/admin/**").hasAnyAuthority("ADMIN_BANK_TELLER", "ADMIN_BANK_MANAGER", "ADMIN_DEV")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation().newSession()
                        .maximumSessions(1)
                        .maxSessionsPreventsLogin(true));
        return http.build();
    }
}
