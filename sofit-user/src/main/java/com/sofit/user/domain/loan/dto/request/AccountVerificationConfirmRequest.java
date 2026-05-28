package com.sofit.user.domain.loan.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountVerificationConfirmRequest {

    @NotBlank(message = "인증번호는 필수입니다")
    @Pattern(regexp = "^[0-9]{3}$", message = "인증번호는 3자리 숫자여야 합니다")
    private String verificationCode;
}
