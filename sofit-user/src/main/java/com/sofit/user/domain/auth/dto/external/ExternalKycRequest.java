package com.sofit.user.domain.auth.dto.external;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExternalKycRequest {

    private String businessNumber;
}
