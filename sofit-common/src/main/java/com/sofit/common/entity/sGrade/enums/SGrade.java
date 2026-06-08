package com.sofit.common.entity.sGrade.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 성장 S등급
 */
@Getter
@AllArgsConstructor
public enum SGrade {

    S1("S1"),
    S2("S2"),
    S3("S3"),
    S4("S4"),
    S5("S5"),
    S6("S6"),
    S7("S7"),
    S8("S8"),
    S9("S9"),
    S10("S10");

    private final String label;
}
