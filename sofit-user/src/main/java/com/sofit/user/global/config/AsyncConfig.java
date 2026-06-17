package com.sofit.user.global.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * MDC(traceId, sourceSystem 등)를 비동기 스레드로 전파하는 TaskDecorator.
     * @Async 실행 시 부모 스레드의 MDC 컨텍스트를 자식 스레드에 복사하고,
     * 작업 완료 후 자식 스레드의 MDC를 정리한다.
     */
    private static final TaskDecorator MDC_PROPAGATING_DECORATOR = runnable -> {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        return () -> {
            try {
                if (mdcContext != null) {
                    MDC.setContextMap(mdcContext);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    };

    @Bean(name = "sGradeExecutor")
    public Executor sGradeExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(150);
        executor.setThreadNamePrefix("sgrade-async-");
        executor.setTaskDecorator(MDC_PROPAGATING_DECORATOR);
        executor.initialize();
        return executor;
    }
}
