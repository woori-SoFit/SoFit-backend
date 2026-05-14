package com.sofit.common.entity.loan;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IncomeType {
    SALARY("01"),       // 급여
    BUSINESS("02"),     // 사업
    OTHER("03");        // 기타

    private final String code;
}
