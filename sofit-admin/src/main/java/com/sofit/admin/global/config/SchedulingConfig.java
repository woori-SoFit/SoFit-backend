package com.sofit.admin.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화 설정.
 * 대출 심사 배치(loanDecisionJob) 등 정기 배치 실행에 사용된다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
