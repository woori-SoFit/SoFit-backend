package com.sofit.user.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FinancialCertLookupRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String holderName;

    @NotBlank(message = "주민등록번호 앞 7자리는 필수입니다.")
    @Pattern(regexp = "^\\d{7}$", message = "주민등록번호 앞 7자리는 정확히 7자리 숫자여야 합니다.")
    private String residentNumber;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{11}$", message = "전화번호는 하이픈 없는 11자리 숫자여야 합니다.")
    private String phoneNumber;
}
