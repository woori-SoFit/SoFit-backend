package com.sofit.user.domain.loan.dto.response;

import java.time.LocalDate;

import com.sofit.common.entity.loan.enums.ApplicationStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoanApplicationListResponse {

    private Long applicationId;
    private String productName;
    private ApplicationStatus status;
    private Long requestedAmount;
    private LocalDate appliedAt;  // LocalDateTime → LocalDate 변환
}
