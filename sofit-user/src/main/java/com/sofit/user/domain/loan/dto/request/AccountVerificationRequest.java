package com.sofit.user.domain.loan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountVerificationRequest {
    @NotBlank(message = "계좌번호는 필수입니다")
    @Pattern(regexp = "^[0-9]{7,20}$", message = "계좌번호는 숫자 7~20자리여야 합니다")
    private String accountNumber;
}
