package com.sofit.user.domain.report.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.entity.sGrade.enums.SGrade;
import com.sofit.common.entity.user.User;
import com.sofit.user.domain.report.dto.response.GradeDetailResponse;
import com.sofit.user.domain.report.dto.response.GradeResponse;

class ReportConverterTest {

    // ===================== toGradeResponse 테스트 =====================

    @Test
    @DisplayName("toGradeResponse - S3 등급 → 올바른 comment와 commentDetail이 매핑된다")
    void toGradeResponse_withS3Grade_mapsCommentCorrectly() {
        // given
        SGradeReport sGradeReport = createSGradeReport(
                1L, 10L, SGrade.S3,
                List.of("매출 성장", "업종 순위"), List.of("현금흐름"),
                "현금흐름 안정화를 통해 등급을 높이세요.",
                LocalDateTime.of(2024, 5, 15, 9, 0, 0)
        );

        // when
        GradeResponse response = ReportConverter.toGradeResponse(sGradeReport);

        // then
        assertThat(response).isNotNull();
        assertThat(response.evaluationId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.sGrade()).isEqualTo("S3");
        assertThat(response.comment()).isEqualTo("안정적으로 성장하고 있는 우수 사업장입니다.");
        assertThat(response.commentDetail()).contains("지속적인 매출 성장");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2024, 5, 15, 9, 0, 0));
    }

    @Test
    @DisplayName("toGradeResponse - S1 등급 → 최상위 comment가 매핑된다")
    void toGradeResponse_withS1Grade_mapsTopComment() {
        // given
        SGradeReport sGradeReport = createSGradeReport(
                2L, 20L, SGrade.S1,
                List.of("탁월한 매출 성장세"), List.of(),
                "이 수준을 유지하세요.",
                LocalDateTime.of(2024, 6, 1, 12, 0, 0)
        );

        // when
        GradeResponse response = ReportConverter.toGradeResponse(sGradeReport);

        // then
        assertThat(response.sGrade()).isEqualTo("S1");
        assertThat(response.comment()).isEqualTo("최상위 성장 잠재력을 보유한 우수 사업장입니다.");
        assertThat(response.commentDetail()).contains("최고 수준의 신용도");
    }

    @Test
    @DisplayName("toGradeResponse - S10 등급 → 최하위 comment가 매핑된다")
    void toGradeResponse_withS10Grade_mapsBottomComment() {
        // given
        SGradeReport sGradeReport = createSGradeReport(
                3L, 30L, SGrade.S10,
                List.of(), List.of("매출 회복", "재무 구조 개선"),
                "적극적인 조치가 필요합니다.",
                LocalDateTime.of(2024, 6, 2, 8, 0, 0)
        );

        // when
        GradeResponse response = ReportConverter.toGradeResponse(sGradeReport);

        // then
        assertThat(response.sGrade()).isEqualTo("S10");
        assertThat(response.comment()).isEqualTo("집중적인 관리가 필요한 사업장입니다.");
        assertThat(response.commentDetail()).contains("매출 회복");
    }

    @Test
    @DisplayName("toGradeResponse - 모든 S등급(S1~S10)에서 comment와 commentDetail이 비어있지 않다")
    void toGradeResponse_forAllGrades_commentIsNotBlank() {
        for (SGrade grade : SGrade.values()) {
            // given
            SGradeReport explanation = createSGradeReport(
                    1L, 1L, grade,
                    List.of(), List.of(), "조언입니다.",
                    LocalDateTime.now()
            );

            // when
            GradeResponse response = ReportConverter.toGradeResponse(explanation);

            // then
            assertThat(response.comment())
                    .as("등급 %s의 comment가 비어있습니다.", grade.name())
                    .isNotBlank();
            assertThat(response.commentDetail())
                    .as("등급 %s의 commentDetail이 비어있습니다.", grade.name())
                    .isNotBlank();
        }
    }

    // ===================== toGradeDetailResponse 테스트 =====================

    @Test
    @DisplayName("toGradeDetailResponse - S등급, strengthKeywords, improvementKeywords, advice가 정확히 매핑된다")
    void toGradeDetailResponse_mapsAllFieldsCorrectly() {
        // given
        List<String> strengthKeywords = List.of("매출 성장", "고객 재방문율", "업종 순위");
        List<String> improvementKeywords = List.of("현금흐름 관리", "비용 구조");
        String advice = "현금흐름을 안정화하고 비용 구조를 개선하여 등급 향상을 노려보세요.";

        SGradeReport sGradeReport = createSGradeReport(
                1L, 10L, SGrade.S5,
                strengthKeywords, improvementKeywords, advice,
                LocalDateTime.of(2024, 5, 20, 9, 0, 0)
        );

        // when
        GradeDetailResponse response = ReportConverter.toGradeDetailResponse(sGradeReport);

        // then
        assertThat(response).isNotNull();
        assertThat(response.sGrade()).isEqualTo("S5");
        assertThat(response.strengthKeywords()).containsExactlyElementsOf(strengthKeywords);
        assertThat(response.improvementKeywords()).containsExactlyElementsOf(improvementKeywords);
        assertThat(response.advice()).isEqualTo(advice);
    }

    @Test
    @DisplayName("toGradeDetailResponse - strengthKeywords가 빈 리스트여도 정상 매핑된다")
    void toGradeDetailResponse_withEmptyStrengthKeywords_returnsEmptyList() {
        // given
        SGradeReport sGradeReport = createSGradeReport(
                1L, 10L, SGrade.S8,
                List.of(), List.of("매출 개선"), "매출 안정화가 필요합니다.",
                LocalDateTime.now()
        );

        // when
        GradeDetailResponse response = ReportConverter.toGradeDetailResponse(sGradeReport);

        // then
        assertThat(response.strengthKeywords()).isEmpty();
        assertThat(response.improvementKeywords()).containsExactly("매출 개선");
    }

    @Test
    @DisplayName("toGradeDetailResponse - improvementKeywords가 빈 리스트여도 정상 매핑된다")
    void toGradeDetailResponse_withEmptyImprovementKeywords_returnsEmptyList() {
        // given
        SGradeReport sGradeReport = createSGradeReport(
                1L, 10L, SGrade.S2,
                List.of("탁월한 매출", "안정적 현금흐름"), List.of(), "이 수준을 유지하세요.",
                LocalDateTime.now()
        );

        // when
        GradeDetailResponse response = ReportConverter.toGradeDetailResponse(sGradeReport);

        // then
        assertThat(response.strengthKeywords()).containsExactly("탁월한 매출", "안정적 현금흐름");
        assertThat(response.improvementKeywords()).isEmpty();
    }

    @Test
    @DisplayName("toGradeDetailResponse - sGrade가 enum name()으로 변환된다")
    void toGradeDetailResponse_sGradeIsEnumName() {
        for (SGrade grade : SGrade.values()) {
            // given
            SGradeReport sGradeReport = createSGradeReport(
                    1L, 1L, grade, List.of(), List.of(), "조언.", LocalDateTime.now()
            );

            // when
            GradeDetailResponse response = ReportConverter.toGradeDetailResponse(sGradeReport);

            // then
            assertThat(response.sGrade())
                    .as("등급 %s가 올바르게 변환되지 않았습니다.", grade.name())
                    .isEqualTo(grade.name());
        }
    }

    // ===================== 테스트 픽스처 =====================

    private SGradeReport createSGradeReport(Long evaluationId, Long userId, SGrade sGrade,
                                                   List<String> strengthKeywords,
                                                   List<String> improvementKeywords,
                                                   String advice,
                                                   LocalDateTime createdAt) {
        try {
            var constructor = SGradeReport.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            SGradeReport sGradeReport = constructor.newInstance();

            setField(sGradeReport, "sGradeId", evaluationId);
            setField(sGradeReport, "sGrade", sGrade);
            setField(sGradeReport, "strengthKeywords", strengthKeywords);
            setField(sGradeReport, "improvementKeywords", improvementKeywords);
            setField(sGradeReport, "userAdvice", advice);
            setField(sGradeReport, "createdAt", createdAt);

            User user = createUser(userId);
            setField(sGradeReport, "user", user);

            return sGradeReport;
        } catch (Exception e) {
            throw new RuntimeException("SGradeReport 테스트 데이터 생성 실패", e);
        }
    }

    private User createUser(Long userId) {
        try {
            var constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            setField(user, "userId", userId);
            return user;
        } catch (Exception e) {
            throw new RuntimeException("User 테스트 데이터 생성 실패", e);
        }
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없습니다: " + fieldName);
    }
}
