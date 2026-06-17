package com.sofit.admin.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SoFit Admin API")
                        .description("소상공인 대출 플랫폼 SoFit - 관리자용 API (은행원/개발자)")
                        .version("v1.0.0"));
    }
}
