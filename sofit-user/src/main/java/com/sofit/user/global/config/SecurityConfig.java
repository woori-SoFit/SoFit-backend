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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SessionValidationFilter sessionValidationFilter;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI 허용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // 인증 불필요 경로
                        .requestMatchers("/api/auth/signup/**", "/api/auth/login").permitAll()
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
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
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
