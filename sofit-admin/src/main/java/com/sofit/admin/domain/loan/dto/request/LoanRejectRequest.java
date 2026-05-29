package com.sofit.admin.domain.loan.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoanRejectRequest {

    @NotBlank(message = "comment는 필수입니다.")
    private String comment;
}
