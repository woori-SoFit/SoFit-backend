package com.sofit.user.domain.loan.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ContractRequest {

    @NotNull(message = "신청 금액은 필수입니다.")
    @Positive(message = "신청 금액은 양수여야 합니다.")
    private Long requestedAmount;
}
