package com.sofit.user.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.entity.sGrade.enums.SGrade;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.sGrade.SGradeReportRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.user.domain.report.dto.response.GradeDetailResponse;
import com.sofit.user.domain.report.dto.response.GradeResponse;
import com.sofit.user.domain.report.dto.response.MybizStatusResponse;
import com.sofit.user.domain.report.exception.ReportErrorCode;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @InjectMocks
    private ReportServiceImpl reportService;

    @Mock
    private SGradeReportRepository sGradeReportRepository;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    private static final Long USER_ID = 1L;

    // ===================== findGrade 테스트 =====================

    @Test
    @DisplayName("findGrade - 성장 S등급 결과 존재 시 GradeResponse를 반환한다")
    void findGrade_whenExplanationExists_returnsGradeResponse() {
        // given
        SGradeReport sGradeReport = createSGradeReport(1L, USER_ID, SGrade.S3,
                List.of("매출 성장", "고객 재방문율"),
                List.of("현금흐름"),
                "매출 성장을 지속하면서 현금흐름 관리를 강화하세요.");

        given(sGradeReportRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.of(sGradeReport));

        // when
        GradeResponse response = reportService.findGrade(USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.evaluationId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.sGrade()).isEqualTo("S3");
        assertThat(response.comment()).isNotBlank();
        assertThat(response.commentDetail()).isNotBlank();
        verify(sGradeReportRepository).findLatestCompletedByUserId(USER_ID);
    }

    @Test
    @DisplayName("findGrade - S등급이 S1일 때 최상위 코멘트를 반환한다")
    void findGrade_whenGradeIsS1_returnsTopGradeComment() {
        // given
        SGradeReport sGradeReport = createSGradeReport(2L, USER_ID, SGrade.S1,
                List.of("탁월한 매출"), List.of(), "계속 성장하세요.");

        given(sGradeReportRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.of(sGradeReport));

        // when
        GradeResponse response = reportService.findGrade(USER_ID);

        // then
        assertThat(response.sGrade()).isEqualTo("S1");
        assertThat(response.comment()).isEqualTo("최상위 성장 잠재력을 보유한 우수 사업장입니다.");
        assertThat(response.commentDetail()).contains("최고 수준의 신용도");
    }

    @Test
    @DisplayName("findGrade - S등급이 S10일 때 최하위 코멘트를 반환한다")
    void findGrade_whenGradeIsS10_returnsBottomGradeComment() {
        // given
        SGradeReport sGradeReport = createSGradeReport(3L, USER_ID, SGrade.S10,
                List.of(), List.of("매출 회복 필요"), "적극적인 조치가 필요합니다.");

        given(sGradeReportRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.of(sGradeReport));

        // when
        GradeResponse response = reportService.findGrade(USER_ID);

        // then
        assertThat(response.sGrade()).isEqualTo("S10");
        assertThat(response.comment()).isEqualTo("집중적인 관리가 필요한 사업장입니다.");
        assertThat(response.commentDetail()).contains("매출 회복");
    }

    @Test
    @DisplayName("findGrade - 성장 S등급 미산출 시 GRADE_NOT_FOUND 예외를 던진다")
    void findGrade_whenExplanationNotFound_throwsGradeNotFoundException() {
        // given
        given(sGradeReportRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reportService.findGrade(USER_ID))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> {
                    BaseException baseException = (BaseException) e;
                    assertThat(baseException.getErrorCode()).isEqualTo(ReportErrorCode.GRADE_NOT_FOUND);
                });
    }

    // ===================== findGradeDetail 테스트 =====================

    @Test
    @DisplayName("findGradeDetail - 성장 S등급 결과 존재 시 GradeDetailResponse를 반환한다")
    void findGradeDetail_whenExplanationExists_returnsGradeDetailResponse() {
        // given
        List<String> strengthKeywords = List.of("매출 성장", "고객 재방문율");
        List<String> improvementKeywords = List.of("현금흐름 관리");
        String advice = "현금흐름 관리를 강화하고 비용 구조를 개선하세요.";

        SGradeReport explanation = createSGradeReport(1L, USER_ID, SGrade.S4,
                strengthKeywords, improvementKeywords, advice);

        given(sGradeReportRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.of(explanation));

        // when
        GradeDetailResponse response = reportService.findGradeDetail(USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.sGrade()).isEqualTo("S4");
        assertThat(response.strengthKeywords()).containsExactlyElementsOf(strengthKeywords);
        assertThat(response.improvementKeywords()).containsExactlyElementsOf(improvementKeywords);
        assertThat(response.advice()).isEqualTo(advice);
        verify(sGradeReportRepository).findLatestCompletedByUserId(USER_ID);
    }

    @Test
    @DisplayName("findGradeDetail - strengthKeywords와 improvementKeywords가 비어있어도 정상 반환한다")
    void findGradeDetail_whenKeywordsAreEmpty_returnsEmptyLists() {
        // given
        SGradeReport explanation = createSGradeReport(1L, USER_ID, SGrade.S5,
                List.of(), List.of(), "꾸준히 유지하세요.");

        given(sGradeReportRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.of(explanation));

        // when
        GradeDetailResponse response = reportService.findGradeDetail(USER_ID);

        // then
        assertThat(response.strengthKeywords()).isEmpty();
        assertThat(response.improvementKeywords()).isEmpty();
        assertThat(response.advice()).isEqualTo("꾸준히 유지하세요.");
    }

    @Test
    @DisplayName("findGradeDetail - 성장 S등급 미산출 시 GRADE_NOT_FOUND 예외를 던진다")
    void findGradeDetail_whenExplanationNotFound_throwsGradeNotFoundException() {
        // given
        given(sGradeReportRepository.findLatestCompletedByUserId(USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reportService.findGradeDetail(USER_ID))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> {
                    BaseException baseException = (BaseException) e;
                    assertThat(baseException.getErrorCode()).isEqualTo(ReportErrorCode.GRADE_NOT_FOUND);
                });
    }

    // ===================== findMybizStatus 테스트 =====================

    @Test
    @DisplayName("findMybizStatus - 마이비즈 연동 완료 시 isMybizConnected=true 반환한다")
    void findMybizStatus_whenMybizConnected_returnsTrueStatus() {
        // given
        BusinessProfile profile = createBusinessProfile(true);
        given(businessProfileRepository.findByUser_UserId(USER_ID))
                .willReturn(Optional.of(profile));

        // when
        MybizStatusResponse response = reportService.findMybizStatus(USER_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.isMybizConnected()).isTrue();
        verify(businessProfileRepository).findByUser_UserId(USER_ID);
    }

    @Test
    @DisplayName("findMybizStatus - 마이비즈 미연동 시 isMybizConnected=false 반환한다")
    void findMybizStatus_whenMybizNotConnected_returnsFalseStatus() {
        // given
        BusinessProfile profile = createBusinessProfile(false);
        given(businessProfileRepository.findByUser_UserId(USER_ID))
                .willReturn(Optional.of(profile));

        // when
        MybizStatusResponse response = reportService.findMybizStatus(USER_ID);

        // then
        assertThat(response.isMybizConnected()).isFalse();
    }

    @Test
    @DisplayName("findMybizStatus - 사업자 프로필 미존재 시 isMybizConnected=false 반환한다")
    void findMybizStatus_whenBusinessProfileNotFound_returnsFalseStatus() {
        // given
        given(businessProfileRepository.findByUser_UserId(USER_ID))
                .willReturn(Optional.empty());

        // when
        MybizStatusResponse response = reportService.findMybizStatus(USER_ID);

        // then
        assertThat(response.isMybizConnected()).isFalse();
    }

    // ===================== 테스트 픽스처 =====================

    /**
     * 리플렉션을 사용하여 SGradeReport 테스트 인스턴스를 생성한다.
     */
    private SGradeReport createSGradeReport(Long evaluationId, Long userId, SGrade sGrade,
                                                   List<String> strengthKeywords,
                                                   List<String> improvementKeywords,
                                                   String advice) {
        try {
            var constructor = SGradeReport.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            SGradeReport explanation = constructor.newInstance();

            setField(explanation, "sGradeId", evaluationId);
            setField(explanation, "sGrade", sGrade);
            setField(explanation, "strengthKeywords", strengthKeywords);
            setField(explanation, "improvementKeywords", improvementKeywords);
            setField(explanation, "userAdvice", advice);
            setField(explanation, "createdAt", LocalDateTime.of(2024, 5, 1, 10, 0, 0));

            // User 픽스처 세팅
            User user = createUser(userId);
            setField(explanation, "user", user);

            return explanation;
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

    private BusinessProfile createBusinessProfile(boolean isMybizConnected) {
        try {
            var constructor = BusinessProfile.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            BusinessProfile profile = constructor.newInstance();
            setField(profile, "isMybizConnected", isMybizConnected);
            return profile;
        } catch (Exception e) {
            throw new RuntimeException("BusinessProfile 테스트 데이터 생성 실패", e);
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
