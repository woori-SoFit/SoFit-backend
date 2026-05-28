package com.sofit.user.domain.loan.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 코데프 API 설정값
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "codef")
public class CodefProperties {

    private String clientId;
    private String clientSecret;
    private String baseUrl;
    private String oauthUrl;
}
