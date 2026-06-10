package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationDetailResponse;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationItemResponse;
import com.sofit.admin.domain.loan.dto.response.LoanDashboardResponse;
import com.sofit.common.entity.loan.LoanApplication;
import org.springframework.data.domain.Page;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public class LoanDashboardConverter {

    private LoanDashboardConverter() {
    }

    public static LoanDashboardResponse toLoanDashboardResponse(
            Page<LoanApplication> page,
            Map<Long, String> businessNameMap,
            Map<Long, String> bankerNameMap,
            Map<Long, Long> approvedAmountMap) {

        return new LoanDashboardResponse(
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize(),
                page.getContent().stream()
                        .map(app -> toLoanApplicationItemResponse(
                                app,
                                businessNameMap.get(app.getUser().getUserId()),
                                bankerNameMap.get(app.getAssignedBankerId()),
                                approvedAmountMap.get(app.getApplicationId())
                        ))
                        .toList()
        );
    }

    public static LoanApplicationItemResponse toLoanApplicationItemResponse(
            LoanApplication app,
            String businessName,
            String assigneeName,
            Long approvedAmount) {

        return new LoanApplicationItemResponse(
                app.getApplicationId(),
                app.getAppliedAt(),
                app.getUser().getName(),
                businessName,
                app.getProduct().getProductName(),
                app.getStatus(),
                app.getAssignedBankerId(),
                assigneeName,
                app.getRequestedAmount(),
                approvedAmount
        );
    }

    public static LoanApplicationDetailResponse toLoanApplicationDetailResponse(
            LoanApplication app,
            String businessName,
            String assigneeName) {

        String appliedAtFormatted = app.getAppliedAt() != null
                ? app.getAppliedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : null;

        return new LoanApplicationDetailResponse(
                app.getApplicationId(),
                app.getUser().getName(),
                businessName,
                app.getProduct().getProductName(),
                app.getStatus().name(),
                appliedAtFormatted,
                app.getAssignedBankerId(),
                assigneeName
        );
    }
}
