package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.LoanApplicationGradeResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.sGrade.SGradeReport;
import com.sofit.common.entity.sGrade.Scb;
import com.sofit.common.entity.sGrade.enums.SGrade;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.sGrade.SGradeReportRepository;
import com.sofit.common.repository.sGrade.ScbRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoanApplicationGradeServiceImpl 단위 테스트")
class LoanApplicationGradeServiceImplTest {

    @InjectMocks
    private LoanApplicationGradeServiceImpl loanApplicationGradeService;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private ScbRepository scbRepository;

    @Mock
    private SGradeReportRepository sGradeReportRepository;

    @Nested
    @DisplayName("findLoanApplicationGrade")
    class FindLoanApplicationGradeTest {

        @Test
        @DisplayName("Scb가 존재하지 않으면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenScbNotExists() {
            // given
            given(scbRepository.findByApplicationId(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationGradeService.findLoanApplicationGrade(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("Scb의 sGrade가 null이면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenSGradeIsNull() {
            // given
            Scb scb = mock(Scb.class);
            given(scb.getSGrade()).willReturn(null);
            given(scbRepository.findByApplicationId(1L)).willReturn(Optional.of(scb));

            // when & then
            assertThatThrownBy(() -> loanApplicationGradeService.findLoanApplicationGrade(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("Scb의 sGrade가 유효하지 않은 값이면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenSGradeIsInvalid() {
            // given
            Scb scb = mock(Scb.class);
            given(scb.getSGrade()).willReturn("INVALID_GRADE");
            given(scbRepository.findByApplicationId(1L)).willReturn(Optional.of(scb));

            // when & then
            assertThatThrownBy(() -> loanApplicationGradeService.findLoanApplicationGrade(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("Scb의 sGrade가 빈 문자열이면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenSGradeIsBlank() {
            // given
            Scb scb = mock(Scb.class);
            given(scb.getSGrade()).willReturn("   ");
            given(scbRepository.findByApplicationId(1L)).willReturn(Optional.of(scb));

            // when & then
            assertThatThrownBy(() -> loanApplicationGradeService.findLoanApplicationGrade(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("sEvaluationId가 존재하지 않으면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenSEvaluationIdNotExists() {
            // given
            Scb scb = mock(Scb.class);
            given(scb.getSGrade()).willReturn("S3");
            given(scbRepository.findByApplicationId(1L)).willReturn(Optional.of(scb));
            given(loanApplicationRepository.findSGradeIdByApplicationId(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationGradeService.findLoanApplicationGrade(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("SGradeReport가 존재하지 않으면 NOT_FOUND 예외를 던진다")
        void shouldThrowNotFoundWhenSGradeReportNotExists() {
            // given
            Scb scb = mock(Scb.class);
            given(scb.getSGrade()).willReturn("S3");
            given(scbRepository.findByApplicationId(1L)).willReturn(Optional.of(scb));
            given(loanApplicationRepository.findSGradeIdByApplicationId(1L)).willReturn(Optional.of(100L));
            given(sGradeReportRepository.findById(100L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> loanApplicationGradeService.findLoanApplicationGrade(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorCode")
                    .extracting("code")
                    .isEqualTo("COMMON4004");
        }

        @Test
        @DisplayName("정상 조회 시 LoanApplicationGradeResponse를 반환한다")
        void shouldReturnGradeResponseSuccessfully() {
            // given
            Scb scb = mock(Scb.class);
            given(scb.getSGrade()).willReturn("S3");
            given(scb.getCbScore()).willReturn(750);
            given(scb.getScbScore()).willReturn(800);
            given(scb.getScoreAddition()).willReturn(50);
            given(scbRepository.findByApplicationId(1L)).willReturn(Optional.of(scb));

            given(loanApplicationRepository.findSGradeIdByApplicationId(1L)).willReturn(Optional.of(100L));

            SGradeReport sGradeReport = mock(SGradeReport.class);
            given(sGradeReport.getSGrade()).willReturn(SGrade.S3);
            given(sGradeReport.getTargetGrade()).willReturn(SGrade.S2);
            given(sGradeReport.getStrengthKeywords()).willReturn(List.of("매출 성장", "현금 흐름"));
            given(sGradeReport.getImprovementKeywords()).willReturn(List.of("업종 순위"));
            given(sGradeReport.getStrengthDetails()).willReturn(Map.of("매출 성장", 0.35));
            given(sGradeReport.getImprovementDetails()).willReturn(Map.of("업종 순위", -0.15));
            given(sGradeReport.getAdminAdvice()).willReturn("매출 성장세를 유지하세요.");
            given(sGradeReportRepository.findById(100L)).willReturn(Optional.of(sGradeReport));

            // when
            LoanApplicationGradeResponse response = loanApplicationGradeService.findLoanApplicationGrade(1L);

            // then
            assertThat(response.cbScore().score()).isEqualTo(750);
            assertThat(response.cbScore().maxScore()).isEqualTo(1000);
            assertThat(response.sGrade()).isEqualTo("S3");
            assertThat(response.scbInfo().score()).isEqualTo(800);
            assertThat(response.scbInfo().bonusPoints()).isEqualTo(50);
            assertThat(response.shapResult().grade()).isEqualTo("S3");
            assertThat(response.shapResult().targetGrade()).isEqualTo("S2");
            assertThat(response.shapResult().strengthKeywords()).containsExactly("매출 성장", "현금 흐름");
            assertThat(response.shapResult().improvementKeywords()).containsExactly("업종 순위");
            assertThat(response.shapResult().advice()).isEqualTo("매출 성장세를 유지하세요.");
        }
    }
}
