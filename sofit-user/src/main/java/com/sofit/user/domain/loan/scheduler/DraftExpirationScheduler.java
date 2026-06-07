package com.sofit.user.domain.loan.scheduler;

import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * DRAFT 상태 대출 신청 만료 스케줄러
 * - 매일 새벽 3시에 실행
 * - 생성일 기준 7일이 경과한 DRAFT 상태 신청을 EXPIRED로 일괄 변경
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DraftExpirationScheduler {

    private final LoanApplicationRepository loanApplicationRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void expireStaleDrafts() {
        LocalDateTime expiredBefore = LocalDateTime.now().minusDays(7);

        int updatedCount = loanApplicationRepository.bulkUpdateStatusByStatusAndCreatedAtBefore(
                ApplicationStatus.DRAFT,
                ApplicationStatus.EXPIRED,
                expiredBefore
        );

        if (updatedCount > 0) {
            log.debug("[DraftExpirationScheduler] {}건의 DRAFT 신청을 EXPIRED로 변경 완료", updatedCount);
        }
    }
}
