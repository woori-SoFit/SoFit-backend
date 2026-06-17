package com.sofit.admin.domain.dev.controller;

import com.sofit.admin.domain.dev.dto.response.BatchHistoryListResponse;
import com.sofit.admin.domain.dev.exception.DevBatchErrorCode;
import com.sofit.admin.domain.dev.exception.DevBatchSuccessCode;
import com.sofit.admin.domain.dev.service.LoanDecisionBatchService;
import com.sofit.admin.global.util.AdminRoleService;
import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecutionException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대출 심사 배치 관리 컨트롤러.
 * loanDecisionJob 실행 이력 조회 및 수동 트리거 엔드포인트를 제공한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dev/batch/loan-decision")
@RequiredArgsConstructor
public class BatchTriggerController implements BatchTriggerControllerDocs {

    private final JobLauncher jobLauncher;
    private final Job loanDecisionJob;
    private final LoanDecisionBatchService loanDecisionBatchService;
    private final AdminRoleService adminRoleService;

    @GetMapping
    @Override
    public ApiResponse<BatchHistoryListResponse> findLoanDecisionBatchHistories(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        UserRole role = adminRoleService.getCurrentUserRole();
        if (role != UserRole.ADMIN_DEV) {
            throw new BaseException(GeneralErrorCode.FORBIDDEN);
        }

        BatchHistoryListResponse response = loanDecisionBatchService.findBatchHistories(page, size);
        return ApiResponse.onSuccess(DevBatchSuccessCode.LOAN_DECISION_BATCH_HISTORY_OK, response);
    }

    @PostMapping("/trigger")
    @Override
    public ApiResponse<Void> triggerLoanDecisionBatch() {
        UserRole role = adminRoleService.getCurrentUserRole();
        if (role != UserRole.ADMIN_DEV) {
            throw new BaseException(GeneralErrorCode.FORBIDDEN);
        }

        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(loanDecisionJob, params);

            log.info("[BatchTrigger] loanDecisionJob 실행 완료");
            return ApiResponse.onSuccess(DevBatchSuccessCode.LOAN_DECISION_BATCH_TRIGGERED, null);
        } catch (JobExecutionException e) {
            log.error("[BatchTrigger] loanDecisionJob 실행 실패", e);
            throw new BaseException(DevBatchErrorCode.LOAN_DECISION_BATCH_FAILED);
        }
    }
}
