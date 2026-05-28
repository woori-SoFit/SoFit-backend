package com.sofit.admin.domain.loan.controller;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationInfoResponse;
import com.sofit.admin.domain.loan.dto.response.LoanDashboardResponse;
import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
import com.sofit.admin.domain.loan.exception.LoanDashboardSuccessCode;
import com.sofit.admin.domain.loan.service.LoanApplicationInfoService;
import com.sofit.admin.domain.loan.service.LoanDashboardService;
import com.sofit.admin.domain.loan.service.MyBizDataDetailService;
import com.sofit.admin.global.util.SecurityUtil;
import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/admin/loan-applications")
@RequiredArgsConstructor
public class LoanDashboardController implements LoanDashboardControllerDocs {

    private static final Set<ApplicationStatus> ALLOWED_STATUSES = Set.of(
            ApplicationStatus.SYSTEM_APPROVED,
            ApplicationStatus.SYSTEM_HOLD,
            ApplicationStatus.MANAGER_REVIEW,
            ApplicationStatus.APPROVED,
            ApplicationStatus.REJECTED
    );

    private final LoanDashboardService loanDashboardService;
    private final LoanApplicationInfoService loanApplicationInfoService;
    private final MyBizDataDetailService myBizDataDetailService;

    @GetMapping
    @Override
    public ApiResponse<LoanDashboardResponse> findLoanApplications(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) List<String> status,
            @RequestParam(defaultValue = "false") Boolean myOnly) {

        // page/size 유효성 검증
        if (page < 0) {
            throw new BaseException(GeneralErrorCode.BAD_REQUEST);
        }
        if (size < 1 || size > 100) {
            throw new BaseException(GeneralErrorCode.BAD_REQUEST);
        }

        // status 파라미터 검증 및 변환 (다중 상태 지원)
        List<ApplicationStatus> statuses = null;
        if (status != null && !status.isEmpty()) {
            statuses = new ArrayList<>();
            for (String s : status) {
                if (s == null || s.isBlank()) {
                    continue;
                }
                try {
                    ApplicationStatus parsed = ApplicationStatus.valueOf(s);
                    if (!ALLOWED_STATUSES.contains(parsed)) {
                        throw new BaseException(GeneralErrorCode.BAD_REQUEST);
                    }
                    statuses.add(parsed);
                } catch (IllegalArgumentException e) {
                    throw new BaseException(GeneralErrorCode.BAD_REQUEST);
                }
            }
            if (statuses.isEmpty()) {
                statuses = null;
            }
        }

        // 현재 로그인한 은행원 userId 추출
        Long currentUserId = SecurityUtil.getCurrentUserId();

        Pageable pageable = PageRequest.of(page, size);
        LoanDashboardResponse response = loanDashboardService.findLoanApplications(
                statuses, myOnly, currentUserId, pageable);
        return ApiResponse.onSuccess(LoanDashboardSuccessCode.LOAN_DASHBOARD_OK, response);
    }

    @GetMapping("/{applicationId}")
    @Override
    public ApiResponse<LoanApplicationDetailResponse> findLoanApplicationDetail(
            @PathVariable Long applicationId) {
        LoanApplicationDetailResponse response = loanDashboardService.findLoanApplicationDetail(applicationId);
        return ApiResponse.onSuccess(LoanDashboardSuccessCode.LOAN_APPLICATION_DETAIL_OK, response);
    }

    @GetMapping("/{applicationId}/info")
    @Override
    public ApiResponse<LoanApplicationInfoResponse> findLoanApplicationInfo(
            @PathVariable Long applicationId) {
        LoanApplicationInfoResponse response = loanApplicationInfoService.findLoanApplicationInfo(applicationId);
        return ApiResponse.onSuccess(LoanDashboardSuccessCode.LOAN_APPLICATION_INFO_OK, response);
    }

    @GetMapping("/{applicationId}/mybiz-data")
    @Override
    public ApiResponse<MyBizDataDetailResponse> findMyBizDataDetail(
            @PathVariable Long applicationId) {
        MyBizDataDetailResponse response = myBizDataDetailService.findMyBizDataDetail(applicationId);
        return ApiResponse.onSuccess(LoanDashboardSuccessCode.MY_BIZ_DATA_DETAIL_OK, response);
    }
}
