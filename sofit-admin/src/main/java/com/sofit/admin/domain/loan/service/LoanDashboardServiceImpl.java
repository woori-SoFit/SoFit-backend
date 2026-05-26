package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.LoanDashboardConverter;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.admin.domain.loan.dto.response.LoanDashboardResponse;
import com.sofit.admin.domain.loan.exception.LoanDashboardErrorCode;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanDashboardServiceImpl implements LoanDashboardService {

    private static final List<ApplicationStatus> DASHBOARD_STATUSES = List.of(
            ApplicationStatus.SYSTEM_APPROVED,
            ApplicationStatus.SYSTEM_HOLD,
            ApplicationStatus.MANAGER_REVIEW,
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED
    );

    private final LoanApplicationRepository loanApplicationRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;

    @Override
    public LoanDashboardResponse findLoanApplications(List<ApplicationStatus> statuses, Long assignedBankerId, Pageable pageable) {
        // 상태 필터: null 또는 빈 리스트이면 전체 대시보드 상태 조회, 아니면 전달된 상태 목록으로 조회
        if (statuses == null || statuses.isEmpty()) {
            statuses = DASHBOARD_STATUSES;
        }

        // 담당 은행원 ID 필터에 따라 적절한 Repository 메서드 호출
        Page<LoanApplication> page;
        if (assignedBankerId == null) {
            page = loanApplicationRepository.findDashboardApplications(statuses, pageable);
        } else {
            page = loanApplicationRepository.findDashboardApplicationsByBankerId(statuses, assignedBankerId, pageable);
        }

        // 조회된 Page에서 userIds 추출 → BusinessProfile 일괄 조회 → Map<userId, businessName> 변환
        List<Long> userIds = page.getContent().stream()
                .map(app -> app.getUser().getUserId())
                .distinct()
                .toList();

        Map<Long, String> businessNameMap = businessProfileRepository.findByUser_UserIdIn(userIds).stream()
                .collect(Collectors.toMap(
                        bp -> bp.getUser().getUserId(),
                        BusinessProfile::getBusinessName,
                        (existing, replacement) -> existing
                ));

        // bankerNameMap: assignedBankerId → banker name 조회
        List<Long> bankerIds = page.getContent().stream()
                .map(LoanApplication::getAssignedBankerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, String> bankerNameMap = userRepository.findAllById(bankerIds).stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        User::getName,
                        (existing, replacement) -> existing
                ));

        return LoanDashboardConverter.toLoanDashboardResponse(page, businessNameMap, bankerNameMap);
    }

    @Override
    public LoanApplicationDetailResponse findLoanApplicationDetail(Long applicationId) {
        // 1. LoanApplication 조회
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(LoanDashboardErrorCode.LOAN_APPLICATION_NOT_FOUND));

        // 2. BusinessProfile에서 businessName 조회
        String businessName = businessProfileRepository.findByUser_UserId(app.getUser().getUserId())
                .map(BusinessProfile::getBusinessName)
                .orElse(null);

        // 3. assignedBankerId가 존재하면 은행원 이름 조회
        String assigneeName = null;
        if (app.getAssignedBankerId() != null) {
            assigneeName = userRepository.findById(app.getAssignedBankerId())
                    .map(User::getName)
                    .orElse(null);
        }

        // 4. Converter로 DTO 변환
        return LoanDashboardConverter.toLoanApplicationDetailResponse(app, businessName, assigneeName);
    }
}
