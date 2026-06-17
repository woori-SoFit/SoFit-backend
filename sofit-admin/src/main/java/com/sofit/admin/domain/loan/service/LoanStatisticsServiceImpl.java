package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.LoanStatisticsConverter;
import com.sofit.admin.domain.loan.dto.response.LoanStatisticsResponse;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.projection.StatusCountProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanStatisticsServiceImpl implements LoanStatisticsService {

    private final LoanApplicationRepository loanApplicationRepository;

    // 통계 집계 대상 상태 목록
    private static final List<ApplicationStatus> STATISTICS_STATUSES = List.of(
            ApplicationStatus.SYSTEM_APPROVED,
            ApplicationStatus.SYSTEM_REJECTED,
            ApplicationStatus.MANAGER_REVIEW,
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED
    );

    @Override
    public LoanStatisticsResponse getStatistics() {
        List<StatusCountProjection> counts = loanApplicationRepository.countByStatuses(STATISTICS_STATUSES);
        return LoanStatisticsConverter.toLoanStatisticsResponse(counts);
    }
}
