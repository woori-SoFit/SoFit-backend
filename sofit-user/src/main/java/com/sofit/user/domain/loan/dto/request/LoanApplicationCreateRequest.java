package com.sofit.user.domain.loan.dto.request;

import com.sofit.common.entity.loan.enums.IncomeType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoanApplicationCreateRequest {

    @NotNull(message = "연소득 구간은 필수입니다.")
    private String annualIncome;

    @NotNull(message = "신용점수 구간은 필수입니다.")
    private String creditScore;

    @NotNull(message = "소득 유형은 필수입니다.")
    private IncomeType incomeType;

    @NotNull(message = "기존 대출 금액 구간은 필수입니다.")
    private String existingLoanAmt;
}
