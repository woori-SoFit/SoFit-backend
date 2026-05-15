package com.sofit.user.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ExternalMockConfig {

    @Value("${external.mock.url}")
    private String baseUrl;

    @Bean
    public RestClient externalMockRestClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
