package com.sofit.admin.global.batch;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoanDecisionTasklet implements Tasklet {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDecisionProcessor loanDecisionProcessor;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        // S_COMPLETED 상태의 대출 신청 건 조회
        List<LoanApplication> applications =
                loanApplicationRepository.findByStatus(ApplicationStatus.S_COMPLETED);

        log.info("[LoanDecisionBatch] S_COMPLETED 대출 신청 건수: {}", applications.size());

        for (LoanApplication application : applications) {
            try {
                loanDecisionProcessor.processApplication(application);
            } catch (Exception e) {
                log.error("[LoanDecisionBatch] applicationId={} 처리 중 예외 발생: {}",
                        application.getApplicationId(), e.getMessage(), e);
            }
        }

        log.info("[LoanDecisionBatch] 배치 처리 완료");
        return RepeatStatus.FINISHED;
    }
}
