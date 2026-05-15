package com.sofit.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.sofit")
public class SofitUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(SofitUserApplication.class, args);
    }
}