package com.sofit.user.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record FinancialCertVerifyRequest(
        @NotBlank(message = "전화번호는 필수입니다.")
        @Pattern(regexp = "^\\d{11}$", message = "전화번호는 하이픈 없는 11자리 숫자여야 합니다.")
        String phoneNumber,

        @NotBlank(message = "PIN은 필수입니다.")
        @Pattern(regexp = "^\\d{6}$", message = "PIN은 6자리 숫자여야 합니다.")
        String pin
) {
}
