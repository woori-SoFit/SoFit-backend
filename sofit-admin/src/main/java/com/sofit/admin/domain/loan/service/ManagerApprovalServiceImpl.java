package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.ManagerApprovalConverter;
import com.sofit.admin.domain.loan.dto.response.ManagerApprovalListResponse;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerApprovalServiceImpl implements ManagerApprovalService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;

    @Override
    public ManagerApprovalListResponse findManagerReviewApplications() {
        // MANAGER_REVIEW 상태의 대출 신청 건 조회 (appliedAt 오름차순, User/Product JOIN FETCH)
        List<LoanApplication> applications = loanApplicationRepository
                .findByStatusWithUserAndProduct(ApplicationStatus.MANAGER_REVIEW);

        // 빈 결과 시 빈 리스트 포함 응답 반환
        if (applications.isEmpty()) {
            return new ManagerApprovalListResponse(Collections.emptyList());
        }

        // BusinessProfile 일괄 조회 → userId별 businessName 매핑
        List<Long> userIds = applications.stream()
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

        // assignedBankerId 일괄 조회 → bankerName 매핑
        List<Long> bankerIds = applications.stream()
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

        // Converter를 통해 응답 DTO 변환
        return ManagerApprovalConverter.toManagerApprovalListResponse(applications, businessNameMap, bankerNameMap);
    }
}
