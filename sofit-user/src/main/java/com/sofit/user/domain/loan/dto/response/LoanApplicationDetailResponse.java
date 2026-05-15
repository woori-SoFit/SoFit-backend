package com.sofit.user.domain.loan.dto.response;

import java.time.LocalDateTime;

import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.RepaymentMethod;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoanApplicationDetailResponse {

    private Long applicationId;
    private String productName;
    private ApplicationStatus status;
    private Long requestedAmount;
    private Integer requestedTerm;
    private RepaymentMethod repaymentMethod;
    private LocalDateTime appliedAt;
}
