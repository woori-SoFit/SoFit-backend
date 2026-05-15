package com.sofit.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.sofit")
@EnableJpaRepositories(basePackages = "com.sofit.user")
public class SofitUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(SofitUserApplication.class, args);
    }
}
