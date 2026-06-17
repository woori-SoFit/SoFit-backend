package com.sofit.user.domain.report.enums;

import com.sofit.common.entity.sGrade.enums.SGrade;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 성장 S등급별 고정 코멘트 매핑.
 * S1이 가장 높은 등급, S10이 가장 낮은 등급.
 */
@Getter
@AllArgsConstructor
public enum SGradeComment {

    S1("최상위 성장 잠재력을 보유한 우수 사업장입니다.",
            "탁월한 매출 성장세와 안정적인 재무 구조를 바탕으로 최고 수준의 신용도를 인정받았어요."),
    S2("매우 높은 성장 가능성을 가진 사업장입니다.",
            "꾸준한 매출 증가와 건전한 현금흐름이 높은 신용도로 이어지고 있어요."),
    S3("안정적으로 성장하고 있는 우수 사업장입니다.",
            "지속적인 매출 성장과 안정적인 상권을 기반으로 신용도가 향상돼요."),
    S4("양호한 성장세를 보이는 사업장입니다.",
            "매출과 수익이 고르게 성장하고 있어 긍정적인 평가를 받고 있어요."),
    S5("평균 이상의 성장 지표를 보유한 사업장입니다.",
            "업종 평균을 상회하는 실적으로 안정적인 사업 운영이 확인돼요."),
    S6("보통 수준의 성장 가능성을 가진 사업장입니다.",
            "현재 안정적인 운영을 유지하고 있으며, 추가 성장 여력이 있어요."),
    S7("성장 잠재력이 있으나 개선이 필요한 사업장입니다.",
            "일부 지표에서 개선 여지가 보이며, 매출 안정화가 필요해요."),
    S8("성장을 위한 노력이 필요한 사업장입니다.",
            "현금흐름 관리와 매출 다각화를 통해 성장 기반을 강화할 필요가 있어요."),
    S9("적극적인 개선이 필요한 사업장입니다.",
            "재무 안정성 확보와 비용 구조 개선을 통해 사업 체질을 강화해야 해요."),
    S10("집중적인 관리가 필요한 사업장입니다.",
            "매출 회복과 재무 구조 개선을 위한 적극적인 조치가 필요해요.");

    private final String comment;
    private final String commentDetail;

    /**
     * SGrade enum으로 해당 코멘트를 찾는다.
     */
    public static SGradeComment fromGrade(SGrade grade) {
        if (grade == null) {
            return null;
        }
        try {
            return SGradeComment.valueOf(grade.name());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
