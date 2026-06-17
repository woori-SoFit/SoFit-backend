package com.sofit.user.domain.auth.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupCompleteRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,20}$", message = "아이디는 영문/숫자 4~20자여야 합니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
            message = "비밀번호는 영문, 숫자, 특수문자 포함 8~20자여야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "주민번호 앞 7자리는 필수입니다.")
    @Pattern(regexp = "^\\d{7}$", message = "주민번호 앞 7자리 숫자여야 합니다.")
    private String residentNumber;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{11}$", message = "전화번호는 하이픈 없는 11자리 숫자여야 합니다.")
    private String phoneNumber;

    @Valid
    @NotEmpty(message = "약관 동의 항목은 필수입니다.")
    private List<ConsentItem> consents;

    @Getter
    @NoArgsConstructor
    public static class ConsentItem {

        @NotNull(message = "약관 ID는 필수입니다.")
        private Long termId;

        @NotNull(message = "동의 여부는 필수입니다.")
        private Boolean isConsented;
    }
}
