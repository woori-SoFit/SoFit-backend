package com.sofit.admin.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * sofit-admin에서 sofit-user 서버로 내부 HTTP 호출을 위한 RestClient 설정.
 * 알림 푸시 등 서버 간 통신에 사용된다.
 */
@Configuration
public class RestClientConfig {

    @Value("${sofit.user.internal-url}")
    private String userServerUrl;

    @Bean
    public RestClient notificationRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);

        return RestClient.builder()
                .baseUrl(userServerUrl)
                .requestFactory(factory)
                .build();
    }
}
