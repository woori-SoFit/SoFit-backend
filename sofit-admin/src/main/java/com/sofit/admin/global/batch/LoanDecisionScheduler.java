package com.sofit.admin.global.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 대출 심사 배치 스케줄러.
 * 매일 05:00에 loanDecisionJob을 자동 실행한다.
 * Python S등급 배치(03:00)가 완료된 이후에 실행되도록 시간을 설정.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanDecisionScheduler {

    private final JobLauncher jobLauncher;
    private final Job loanDecisionJob;

    @Scheduled(cron = "0 0 5 * * *")
    public void runLoanDecisionJob() {
        log.info("[LoanDecisionScheduler] 대출 심사 배치 스케줄 실행 시작");
        try {
            jobLauncher.run(loanDecisionJob,
                    new JobParametersBuilder()
                            .addLong("timestamp", System.currentTimeMillis())
                            .toJobParameters()
            );
            log.info("[LoanDecisionScheduler] 대출 심사 배치 스케줄 실행 완료");
        } catch (JobExecutionException e) {
            log.error("[LoanDecisionScheduler] 대출 심사 배치 실행 실패", e);
        }
    }
}
