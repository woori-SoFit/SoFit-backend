package com.sofit.admin.domain.dev.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 대출 심사 배치 수동 트리거용 컨트롤러.
 * loanDecisionJob은 @Scheduled로 매일 자동 실행되지만,
 * 관리자가 수동으로 트리거할 수 있는 엔드포인트도 제공한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dev/batch")
@RequiredArgsConstructor
public class BatchTriggerController {

    private final JobLauncher jobLauncher;
    private final Job loanDecisionJob;

    @PostMapping("/loan-decision")
    public ResponseEntity<Map<String, String>> triggerLoanDecisionBatch() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(loanDecisionJob, params);

            log.info("[BatchTrigger] loanDecisionJob 실행 완료");
            return ResponseEntity.ok(Map.of("message", "loanDecisionJob 실행 완료"));
        } catch (JobExecutionException e) {
            log.error("[BatchTrigger] loanDecisionJob 실행 실패", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "loanDecisionJob 실행 실패: " + e.getMessage()));
        }
    }
}
