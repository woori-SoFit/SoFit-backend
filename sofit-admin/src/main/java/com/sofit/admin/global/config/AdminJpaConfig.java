package com.sofit.admin.global.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "com.sofit.admin.domain")
@EnableJpaRepositories(basePackages = "com.sofit.admin.domain")
public class AdminJpaConfig {
}
