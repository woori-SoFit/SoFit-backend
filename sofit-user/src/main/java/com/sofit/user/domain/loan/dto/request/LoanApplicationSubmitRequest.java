package com.sofit.user.domain.loan.dto.request;

import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoanApplicationSubmitRequest {

    @NotNull(message = "대출 용도는 필수입니다.")
    private LoanPurpose purpose;

    @NotNull(message = "상환 방식은 필수입니다.")
    private RepaymentMethod repaymentMethod;

    @NotNull(message = "희망 대출 기간은 필수입니다.")
    @Positive(message = "희망 대출 기간은 0보다 커야 합니다.")
    private Integer requestedTerm;

    @NotNull(message = "희망 대출 금액은 필수입니다.")
    @Positive(message = "희망 대출 금액은 0보다 커야 합니다.")
    private Long requestedAmount;
   
}
