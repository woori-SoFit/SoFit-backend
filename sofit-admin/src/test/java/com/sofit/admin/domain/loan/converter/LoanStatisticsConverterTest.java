package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanStatisticsResponse;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.projection.StatusCountProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LoanStatisticsConverter 단위 테스트")
class LoanStatisticsConverterTest {

    @Test
    @DisplayName("빈 리스트 입력 시 모든 값이 0인 응답을 반환한다")
    void shouldReturnZerosForEmptyList() {
        // when
        LoanStatisticsResponse response = LoanStatisticsConverter.toLoanStatisticsResponse(Collections.emptyList());

        // then
        assertThat(response.pending()).isZero();
        assertThat(response.managerReview()).isZero();
        assertThat(response.approved()).isZero();
        assertThat(response.rejected()).isZero();
    }

    @Test
    @DisplayName("SYSTEM_APPROVED와 SYSTEM_REJECTED를 합산하여 pending으로 반환한다")
    void shouldSumSystemStatusesForPending() {
        // given
        List<StatusCountProjection> counts = List.of(
                createProjection(ApplicationStatus.SYSTEM_APPROVED, 3L),
                createProjection(ApplicationStatus.SYSTEM_REJECTED, 2L)
        );

        // when
        LoanStatisticsResponse response = LoanStatisticsConverter.toLoanStatisticsResponse(counts);

        // then
        assertThat(response.pending()).isEqualTo(5);
    }

    @Test
    @DisplayName("모든 상태가 존재할 때 올바르게 매핑한다")
    void shouldMapAllStatuses() {
        // given
        List<StatusCountProjection> counts = List.of(
                createProjection(ApplicationStatus.SYSTEM_APPROVED, 10L),
                createProjection(ApplicationStatus.SYSTEM_REJECTED, 5L),
                createProjection(ApplicationStatus.MANAGER_REVIEW, 3L),
                createProjection(ApplicationStatus.APPROVED, 20L),
                createProjection(ApplicationStatus.REJECTED, 7L)
        );

        // when
        LoanStatisticsResponse response = LoanStatisticsConverter.toLoanStatisticsResponse(counts);

        // then
        assertThat(response.pending()).isEqualTo(15);
        assertThat(response.managerReview()).isEqualTo(3);
        assertThat(response.approved()).isEqualTo(20);
        assertThat(response.rejected()).isEqualTo(7);
    }

    @Test
    @DisplayName("일부 상태만 존재할 때 없는 상태는 0으로 처리한다")
    void shouldReturnZeroForMissingStatuses() {
        // given
        List<StatusCountProjection> counts = List.of(
                createProjection(ApplicationStatus.SYSTEM_APPROVED, 3L)
        );

        // when
        LoanStatisticsResponse response = LoanStatisticsConverter.toLoanStatisticsResponse(counts);

        // then
        assertThat(response.pending()).isEqualTo(3);
        assertThat(response.managerReview()).isZero();
        assertThat(response.approved()).isZero();
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
