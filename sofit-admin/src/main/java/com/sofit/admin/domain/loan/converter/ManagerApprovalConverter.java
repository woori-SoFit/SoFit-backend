package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.ManagerApprovalItemResponse;
import com.sofit.admin.domain.loan.dto.response.ManagerApprovalListResponse;
import com.sofit.common.entity.loan.LoanApplication;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ManagerApprovalConverter {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ManagerApprovalConverter() {
    }

    public static ManagerApprovalListResponse toManagerApprovalListResponse(
            List<LoanApplication> applications,
            Map<Long, String> businessNameMap,
            Map<Long, String> bankerNameMap) {

        List<ManagerApprovalItemResponse> items = applications.stream()
                .map(app -> toManagerApprovalItemResponse(
                        app,
                        businessNameMap.get(app.getUser().getUserId()),
                        bankerNameMap.get(app.getAssignedBankerId())
                ))
                .toList();

        return new ManagerApprovalListResponse(items);
    }

    public static ManagerApprovalItemResponse toManagerApprovalItemResponse(
            LoanApplication app,
            String businessName,
            String bankerName) {

        String applicationDate = app.getAppliedAt() != null
                ? app.getAppliedAt().format(DATE_FORMATTER)
                : null;

        return new ManagerApprovalItemResponse(
                app.getApplicationId(),
                applicationDate,
                app.getUser().getName(),
                businessName,
                app.getProduct().getProductName(),
                bankerName,
                app.getRequestedAmount()
        );
    }
}
