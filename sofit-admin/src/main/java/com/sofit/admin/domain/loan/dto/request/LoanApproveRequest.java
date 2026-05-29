package com.sofit.admin.domain.loan.dto.request;

import com.sofit.common.entity.loan.enums.RepaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class LoanApproveRequest {

    @NotNull(message = "승인 금액은 필수입니다.")
    @Positive(message = "승인 금액은 0보다 커야 합니다.")
    private Long approvedAmount;

    @NotNull(message = "확정 금리는 필수입니다.")
    @Positive(message = "확정 금리는 0보다 커야 합니다.")
    private BigDecimal approvedRate;

    @NotNull(message = "확정 기간은 필수입니다.")
    @Positive(message = "확정 기간은 0보다 커야 합니다.")
    private Integer approvedTerm;

    @NotNull(message = "상환 방식은 필수입니다.")
    private RepaymentMethod repaymentMethod;

    @NotBlank(message = "comment는 필수입니다.")
    private String comment;
}
