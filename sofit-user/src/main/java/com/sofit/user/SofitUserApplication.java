package com.sofit.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = {"com.sofit.common.entity", "com.sofit.user"})
@EnableJpaRepositories(basePackages = {"com.sofit.common.repository", "com.sofit.user"})
public class SofitUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(SofitUserApplication.class, args);
    }
}