package com.sofit.user.domain.auth.dto.external;

import com.sofit.user.domain.auth.dto.request.FinancialCertLookupRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ExternalFinancialCertLookupRequest {

    private String holderName;
    private String residentNumber;
    private String phoneNumber;

    public static ExternalFinancialCertLookupRequest from(FinancialCertLookupRequest request) {
        return new ExternalFinancialCertLookupRequest(
                request.getHolderName(),
                request.getResidentNumber(),
                request.getPhoneNumber()
        );
    }
}
