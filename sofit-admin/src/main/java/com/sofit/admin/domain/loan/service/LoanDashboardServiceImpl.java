package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.LoanDashboardConverter;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.admin.domain.loan.dto.response.LoanDashboardResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
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
            ApplicationStatus.SYSTEM_REJECTED,
            ApplicationStatus.MANAGER_REVIEW,
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED,
            ApplicationStatus.EXECUTED
    );

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDecisionRepository loanDecisionRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;

    @Override
    public LoanDashboardResponse findLoanApplications(
            List<ApplicationStatus> statuses, Boolean myOnly, Long currentUserId, Pageable pageable) {

        // 상태 필터: statuses가 null이면 전체 대시보드 상태 조회
        List<ApplicationStatus> filterStatuses = (statuses != null && !statuses.isEmpty())
                ? statuses
                : DASHBOARD_STATUSES;

        // myOnly 필터에 따라 Repository 메서드 분기
        Page<LoanApplication> page;
        if (Boolean.TRUE.equals(myOnly)) {
            page = loanApplicationRepository.findDashboardApplicationsByBankerId(
                    filterStatuses, currentUserId, pageable);
        } else {
            page = loanApplicationRepository.findDashboardApplications(filterStatuses, pageable);
        }

        // BusinessProfile 일괄 조회 → userId별 최신 createdAt 선택
        List<Long> userIds = page.getContent().stream()
                .map(app -> app.getUser().getUserId())
                .distinct()
                .toList();

        Map<Long, String> businessNameMap = businessProfileRepository.findByUser_UserIdIn(userIds).stream()
                .collect(Collectors.groupingBy(
                        bp -> bp.getUser().getUserId(),
                        Collectors.collectingAndThen(
                                Collectors.maxBy(Comparator.comparing(BusinessProfile::getCreatedAt)),
                                opt -> opt.map(BusinessProfile::getBusinessName).orElse(null)
                        )
                ));

        // bankerNameMap: assignedBankerId → banker name 일괄 조회
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

        // approvedAmountMap: applicationId → approvedAmount 일괄 조회
        List<Long> applicationIds = page.getContent().stream()
                .map(LoanApplication::getApplicationId)
                .toList();

        Map<Long, Long> approvedAmountMap;
        if (applicationIds.isEmpty()) {
            approvedAmountMap = Map.of();
        } else {
            List<DecisionStatus> approvalStatuses = List.of(
                    DecisionStatus.SYSTEM_APPROVED,
                    DecisionStatus.TELLER_APPROVED,
                    DecisionStatus.MANAGER_APPROVED
            );

            approvedAmountMap = loanDecisionRepository
                    .findByApplication_ApplicationIdInAndStatusInOrderByCreatedAtAsc(applicationIds, approvalStatuses)
                    .stream()
                    .collect(Collectors.toMap(
                            d -> d.getApplication().getApplicationId(),
                            LoanDecision::getApprovedAmount,
                            (existing, replacement) -> replacement // createdAt ASC 정렬이므로 뒤에 오는 값이 최신
                    ));
        }

        return LoanDashboardConverter.toLoanDashboardResponse(page, businessNameMap, bankerNameMap, approvedAmountMap);
    }

    @Override
    public LoanApplicationDetailResponse findLoanApplicationDetail(Long applicationId) {
        // 1. LoanApplication 조회
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

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
