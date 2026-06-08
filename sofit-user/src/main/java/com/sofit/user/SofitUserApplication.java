package com.sofit.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.sofit")
@EnableScheduling
public class SofitUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(SofitUserApplication.class, args);
    }
}
