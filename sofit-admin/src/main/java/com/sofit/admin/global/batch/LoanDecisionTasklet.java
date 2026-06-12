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
        // SUBMITTED 상태의 대출 신청 건 조회
        List<LoanApplication> applications =
                loanApplicationRepository.findByStatus(ApplicationStatus.SUBMITTED);

        log.info("[LoanDecisionBatch] SUBMITTED 대출 신청 건수: {}", applications.size());

        int processedCount = 0;
        for (LoanApplication application : applications) {
            try {
                loanDecisionProcessor.processApplication(application);
                processedCount++;
            } catch (Exception e) {
                log.error("[LoanDecisionBatch] applicationId={} 처리 중 예외 발생: {}",
                        application.getApplicationId(), e.getMessage(), e);
            }
        }

        // StepExecution에 처리 건수 기록 (메타데이터 테이블 WRITE_COUNT에 반영)
        contribution.incrementWriteCount(processedCount);

        log.info("[LoanDecisionBatch] 배치 처리 완료 (처리 건수: {})", processedCount);
        return RepeatStatus.FINISHED;
    }
}
