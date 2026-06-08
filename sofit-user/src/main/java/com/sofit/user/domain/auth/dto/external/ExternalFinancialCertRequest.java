package com.sofit.user.domain.auth.dto.external;

import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExternalFinancialCertRequest {

    private String phoneNumber;
    private String holderName;
    private String residentNumber;
    private String pin;

    public static ExternalFinancialCertRequest from(FinancialCertVerifyRequest request) {
        return new ExternalFinancialCertRequest(
                request.getPhoneNumber(),
                request.getHolderName(),
                request.getResidentNumber(),
                request.getPin()
        );
    }
}
