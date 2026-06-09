package com.sofit.user.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServerConfig {

    @Value("${sofit.ai.server.url}")
    private String baseUrl;

    @Bean
    public RestClient aiServerRestClient() {
        // SimpleClientHttpRequestFactory는 HTTP/1.1만 사용 (HTTP/2 upgrade 시도 없음)
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(30000);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
