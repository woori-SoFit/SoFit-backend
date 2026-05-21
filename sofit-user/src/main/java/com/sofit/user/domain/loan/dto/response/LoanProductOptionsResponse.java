package com.sofit.user.domain.loan.dto.response;

import com.sofit.common.entity.loan.enums.LoanPurpose;
import com.sofit.common.entity.loan.enums.RepaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class LoanProductOptionsResponse {

    private Long productId;
    private String productName;
    private Long minLimit;
    private Long maxLimit;
    private List<LoanOptionItem> loanOptions;

    @Getter
    @Builder
    public static class LoanOptionItem {
        private LoanPurpose purpose;
        private RepaymentMethod repaymentMethod;
        private Integer maxTermMonths;
    }
}
