package com.sofit.admin.global.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class LoanDecisionBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final LoanDecisionTasklet loanDecisionTasklet;

    @Bean
    public Job loanDecisionJob() {
        return new JobBuilder("loanDecisionJob", jobRepository)
                .start(loanDecisionStep())
                .build();
    }

    @Bean
    public Step loanDecisionStep() {
        return new StepBuilder("loanDecisionStep", jobRepository)
                .tasklet(loanDecisionTasklet, transactionManager)
                .build();
    }
}
