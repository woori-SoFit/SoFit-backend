package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationGradeResponse;
import com.sofit.common.entity.sGrade.Scb;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.entity.sGrade.enums.SGrade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("LoanApplicationGradeConverter 단위 테스트")
class LoanApplicationGradeConverterTest {

    @Test
    @DisplayName("정상 데이터로 LoanApplicationGradeResponse를 생성한다")
    void shouldConvertToGradeResponse() {
        // given
        Scb scb = mock(Scb.class);
        given(scb.getCbScore()).willReturn(750);
        given(scb.getScbScore()).willReturn(800);
        given(scb.getScoreAddition()).willReturn(50);

        SGradeReport sGradeReport = mock(SGradeReport.class);
        given(sGradeReport.getSGrade()).willReturn(SGrade.S3);
        given(sGradeReport.getTargetGrade()).willReturn(SGrade.S2);
        given(sGradeReport.getStrengthKeywords()).willReturn(List.of("매출 성장"));
        given(sGradeReport.getImprovementKeywords()).willReturn(List.of("업종 순위"));
        given(sGradeReport.getStrengthDetails()).willReturn(Map.of("매출 성장", 0.35));
        given(sGradeReport.getImprovementDetails()).willReturn(Map.of("업종 순위", -0.15));
        given(sGradeReport.getAdminAdvice()).willReturn("매출 성장세를 유지하세요.");

        // when
        LoanApplicationGradeResponse response = LoanApplicationGradeConverter
                .toLoanApplicationGradeResponse(scb, SGrade.S3, sGradeReport);

        // then
        assertThat(response.cbScore().score()).isEqualTo(750);
        assertThat(response.cbScore().maxScore()).isEqualTo(1000);
        assertThat(response.sGrade()).isEqualTo("S3");
        assertThat(response.scbInfo().score()).isEqualTo(800);
        assertThat(response.scbInfo().maxScore()).isEqualTo(1000);
        assertThat(response.scbInfo().bonusPoints()).isEqualTo(50);
        assertThat(response.shapResult().grade()).isEqualTo("S3");
        assertThat(response.shapResult().targetGrade()).isEqualTo("S2");
        assertThat(response.shapResult().strengthKeywords()).containsExactly("매출 성장");
        assertThat(response.shapResult().improvementKeywords()).containsExactly("업종 순위");
        assertThat(response.shapResult().strengthDetails()).containsEntry("매출 성장", 0.35);
        assertThat(response.shapResult().improvementDetails()).containsEntry("업종 순위", -0.15);
        assertThat(response.shapResult().advice()).isEqualTo("매출 성장세를 유지하세요.");
    }

    @Test
    @DisplayName("null 필드가 있을 때 빈 컬렉션으로 대체한다")
    void shouldHandleNullFields() {
        // given
        Scb scb = mock(Scb.class);
        given(scb.getCbScore()).willReturn(700);
        given(scb.getScbScore()).willReturn(700);
        given(scb.getScoreAddition()).willReturn(0);

        SGradeReport sGradeReport = mock(SGradeReport.class);
        given(sGradeReport.getSGrade()).willReturn(SGrade.S5);
        given(sGradeReport.getTargetGrade()).willReturn(null);
        given(sGradeReport.getStrengthKeywords()).willReturn(null);
        given(sGradeReport.getImprovementKeywords()).willReturn(null);
        given(sGradeReport.getStrengthDetails()).willReturn(null);
        given(sGradeReport.getImprovementDetails()).willReturn(null);
        given(sGradeReport.getAdminAdvice()).willReturn(null);

        // when
        LoanApplicationGradeResponse response = LoanApplicationGradeConverter
                .toLoanApplicationGradeResponse(scb, SGrade.S5, sGradeReport);

        // then
        assertThat(response.shapResult().targetGrade()).isNull();
        assertThat(response.shapResult().strengthKeywords()).isEmpty();
        assertThat(response.shapResult().improvementKeywords()).isEmpty();
        assertThat(response.shapResult().strengthDetails()).isEmpty();
        assertThat(response.shapResult().improvementDetails()).isEmpty();
        assertThat(response.shapResult().advice()).isNull();
    }
}
