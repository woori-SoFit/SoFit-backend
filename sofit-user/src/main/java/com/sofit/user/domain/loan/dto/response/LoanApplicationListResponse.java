package com.sofit.user.domain.loan.dto.response;

import com.sofit.common.entity.loan.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class LoanApplicationListResponse {

    private List<LoanApplicationItem> loanApplications;

    @Getter
    @Builder
    public static class LoanApplicationItem {
        private Long applicationId;
        private String productName;
        private ApplicationStatus status;
        private Long requestedAmount;
        private LocalDate appliedAt;
    }
}
