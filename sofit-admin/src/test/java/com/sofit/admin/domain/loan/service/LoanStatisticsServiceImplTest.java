package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.LoanStatisticsResponse;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.projection.StatusCountProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanStatisticsServiceImpl 단위 테스트")
class LoanStatisticsServiceImplTest {

    @InjectMocks
    private LoanStatisticsServiceImpl loanStatisticsService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Test
    @DisplayName("통계 데이터가 없으면 모든 값이 0인 응답을 반환한다")
    void shouldReturnZerosWhenNoData() {
        // given
        given(loanApplicationRepository.countByStatuses(anyList())).willReturn(Collections.emptyList());

        // when
        LoanStatisticsResponse response = loanStatisticsService.getStatistics();

        // then
        assertThat(response.pending()).isZero();
        assertThat(response.managerReview()).isZero();
        assertThat(response.approved()).isZero();
        assertThat(response.rejected()).isZero();
    }

    @Test
    @DisplayName("pending은 SYSTEM_APPROVED + SYSTEM_REJECTED 합산이다")
    void shouldSumSystemApprovedAndRejectedForPending() {
        // given
        StatusCountProjection systemApproved = createProjection(ApplicationStatus.SYSTEM_APPROVED, 5L);
        StatusCountProjection systemRejected = createProjection(ApplicationStatus.SYSTEM_REJECTED, 3L);
        StatusCountProjection managerReview = createProjection(ApplicationStatus.MANAGER_REVIEW, 2L);
        StatusCountProjection approved = createProjection(ApplicationStatus.APPROVED, 10L);
        StatusCountProjection rejected = createProjection(ApplicationStatus.REJECTED, 4L);

        given(loanApplicationRepository.countByStatuses(anyList()))
                .willReturn(List.of(systemApproved, systemRejected, managerReview, approved, rejected));

        // when
        LoanStatisticsResponse response = loanStatisticsService.getStatistics();

        // then
        assertThat(response.pending()).isEqualTo(8); // 5 + 3
        assertThat(response.managerReview()).isEqualTo(2);
        assertThat(response.approved()).isEqualTo(10);
        assertThat(response.rejected()).isEqualTo(4);
    }

    @Test
    @DisplayName("일부 상태만 존재할 때 나머지는 0으로 처리한다")
    void shouldHandlePartialData() {
        // given
        StatusCountProjection approved = createProjection(ApplicationStatus.APPROVED, 7L);

        given(loanApplicationRepository.countByStatuses(anyList())).willReturn(List.of(approved));

        // when
        LoanStatisticsResponse response = loanStatisticsService.getStatistics();

        // then
        assertThat(response.pending()).isZero();
        assertThat(response.managerReview()).isZero();
        assertThat(response.approved()).isEqualTo(7);
        assertThat(response.rejected()).isZero();
    }

    private StatusCountProjection createProjection(ApplicationStatus status, Long count) {
        return new StatusCountProjection() {
            @Override
            public ApplicationStatus getStatus() {
                return status;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
