package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanStatisticsResponse;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.projection.StatusCountProjection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * StatusCountProjection 목록을 LoanStatisticsResponse로 변환하는 Converter.
 */
public class LoanStatisticsConverter {

    private LoanStatisticsConverter() {
    }

    /**
     * StatusCountProjection 목록을 LoanStatisticsResponse로 변환한다.
     * pending = SYSTEM_APPROVED + SYSTEM_REJECTED 건수 합산
     * managerReview = MANAGER_REVIEW 건수
     * approved = APPROVED 건수
     * rejected = REJECTED 건수
     * 해당 상태가 없으면 0으로 처리한다.
     */
    public static LoanStatisticsResponse toLoanStatisticsResponse(List<StatusCountProjection> counts) {
        Map<ApplicationStatus, Long> countMap = counts.stream()
                .collect(Collectors.toMap(
                        StatusCountProjection::getStatus,
                        StatusCountProjection::getCount
                ));

        int pending = toInt(countMap.getOrDefault(ApplicationStatus.SYSTEM_APPROVED, 0L))
                + toInt(countMap.getOrDefault(ApplicationStatus.SYSTEM_REJECTED, 0L));
        int managerReview = toInt(countMap.getOrDefault(ApplicationStatus.MANAGER_REVIEW, 0L));
        int approved = toInt(countMap.getOrDefault(ApplicationStatus.APPROVED, 0L));
        int rejected = toInt(countMap.getOrDefault(ApplicationStatus.REJECTED, 0L));

        return new LoanStatisticsResponse(pending, managerReview, approved, rejected);
    }

    private static int toInt(Long value) {
        return value != null ? value.intValue() : 0;
    }
}
