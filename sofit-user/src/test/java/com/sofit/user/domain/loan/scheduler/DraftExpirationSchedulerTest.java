package com.sofit.user.domain.loan.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
@DisplayName("DraftExpirationScheduler 단위 테스트")
class DraftExpirationSchedulerTest {

    @InjectMocks
    private DraftExpirationScheduler draftExpirationScheduler;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Test
    @DisplayName("만료 대상이 있으면 bulkUpdate를 호출하고 건수를 반환한다")
    void expireStaleDrafts_만료대상_존재시_bulkUpdate_호출() {
        // given
        given(loanApplicationRepository.bulkUpdateStatusByStatusAndCreatedAtBefore(
                eq(ApplicationStatus.DRAFT),
                eq(ApplicationStatus.EXPIRED),
                any(LocalDateTime.class)))
                .willReturn(5);

        // when
        draftExpirationScheduler.expireStaleDrafts();

        // then
        verify(loanApplicationRepository).bulkUpdateStatusByStatusAndCreatedAtBefore(
                eq(ApplicationStatus.DRAFT),
                eq(ApplicationStatus.EXPIRED),
                any(LocalDateTime.class));
    }

    @Test
    @DisplayName("만료 대상이 없으면 0건을 반환하고 정상 종료한다")
    void expireStaleDrafts_만료대상_없으면_0건_반환() {
        // given
        given(loanApplicationRepository.bulkUpdateStatusByStatusAndCreatedAtBefore(
                eq(ApplicationStatus.DRAFT),
                eq(ApplicationStatus.EXPIRED),
                any(LocalDateTime.class)))
                .willReturn(0);

        // when
        draftExpirationScheduler.expireStaleDrafts();

        // then
        verify(loanApplicationRepository).bulkUpdateStatusByStatusAndCreatedAtBefore(
                eq(ApplicationStatus.DRAFT),
                eq(ApplicationStatus.EXPIRED),
                any(LocalDateTime.class));
    }
}
