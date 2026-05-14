package com.sofit.user.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BusinessVerificationRequest(
        @NotBlank(message = "사업자등록번호는 필수입니다.")
        @Pattern(regexp = "^\\d{10}$", message = "사업자등록번호는 하이픈 없는 10자리 숫자여야 합니다.")
        String businessNumber
) {
}
