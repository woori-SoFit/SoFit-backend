package com.sofit.user.domain.terms.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.common.entity.user.User;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse.ConsentItemResponse;

class TermConverterTest {

    @Nested
    @DisplayName("toListResponse 메서드")
    class ToListResponseTest {

        @Test
        @DisplayName("Term 목록이 TermListResponse의 TermItem 목록으로 변환된다")
        void term_목록이_TermItem으로_변환된다() {
            // given
            LocalDateTime effectiveAt = LocalDateTime.of(2026, 1, 1, 0, 0);
            Term term1 = createTermFull(1L, TermType.PERSONAL_INFO, "v1.0", "개인정보 수집 동의",
                    "/terms/personal_info_v1.0.pdf", true, effectiveAt);
            Term term2 = createTermFull(2L, TermType.LOAN_APPLICATION, "v1.0", "대출 신청 동의",
                    "/terms/loan_application_v1.0.pdf", false, effectiveAt);

            // when
            var response = TermConverter.toListResponse(List.of(term1, term2));

            // then
            assertThat(response.terms()).hasSize(2);

            var first = response.terms().get(0);
            assertThat(first.termId()).isEqualTo(1L);
            assertThat(first.termType()).isEqualTo("PERSONAL_INFO");
            assertThat(first.version()).isEqualTo("v1.0");
            assertThat(first.title()).isEqualTo("개인정보 수집 동의");
            assertThat(first.fileUrl()).isEqualTo("/terms/personal_info_v1.0.pdf");
            assertThat(first.isRequired()).isTrue();
            assertThat(first.effectiveAt()).isEqualTo(effectiveAt);
        }

        @Test
        @DisplayName("빈 목록이 들어오면 빈 TermListResponse를 반환한다")
        void 빈_목록이면_빈_응답을_반환한다() {
            // when
            var response = TermConverter.toListResponse(List.of());

            // then
            assertThat(response.terms()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toConsentHistoryList 메서드")
    class ToConsentHistoryListTest {

        @Test
        @DisplayName("요청의 user, term, application, isConsented가 정확히 매핑된다")
        void 요청_필드가_ConsentHistory에_정확히_매핑된다() {
            // given
            Long userId = 1L;
            Long applicationId = 100L;

            User user = createUser(userId);
            Term term1 = createTerm(10L, TermType.LOAN_APPLICATION, true);
            Term term2 = createTerm(20L, TermType.LOAN_APPLICATION, false);
            LoanApplication application = createApplication(applicationId);
            Map<Long, Term> termMap = Map.of(10L, term1, 20L, term2);

            ConsentCreateRequest request = new ConsentCreateRequest();
            ReflectionTestUtils.setField(request, "termType", TermType.LOAN_APPLICATION);
            ReflectionTestUtils.setField(request, "applicationId", applicationId);

            ConsentCreateRequest.ConsentItem item1 = new ConsentCreateRequest.ConsentItem();
            ReflectionTestUtils.setField(item1, "termId", 10L);
            ReflectionTestUtils.setField(item1, "isConsented", true);

            ConsentCreateRequest.ConsentItem item2 = new ConsentCreateRequest.ConsentItem();
            ReflectionTestUtils.setField(item2, "termId", 20L);
            ReflectionTestUtils.setField(item2, "isConsented", false);

            ReflectionTestUtils.setField(request, "consents", List.of(item1, item2));

            // when
            List<ConsentHistory> result = TermConverter.toConsentHistoryList(user, termMap, application, request.getConsents());

            // then
            assertThat(result).hasSize(2);

            ConsentHistory first = result.get(0);
            assertThat(first.getUser().getUserId()).isEqualTo(userId);
            assertThat(first.getTerm().getTermId()).isEqualTo(10L);
            assertThat(first.getApplication().getApplicationId()).isEqualTo(applicationId);
            assertThat(first.getIsConsented()).isTrue();

            ConsentHistory second = result.get(1);
            assertThat(second.getUser().getUserId()).isEqualTo(userId);
            assertThat(second.getTerm().getTermId()).isEqualTo(20L);
            assertThat(second.getApplication().getApplicationId()).isEqualTo(applicationId);
            assertThat(second.getIsConsented()).isFalse();
        }

        @Test
        @DisplayName("application이 null이면 ConsentHistory의 application도 null이다")
        void application이_null이면_null로_매핑된다() {
            // given
            Long userId = 2L;

            User user = createUser(userId);
            Term term = createTerm(5L, TermType.PERSONAL_INFO, true);
            Map<Long, Term> termMap = Map.of(5L, term);

            ConsentCreateRequest request = new ConsentCreateRequest();
            ReflectionTestUtils.setField(request, "termType", TermType.PERSONAL_INFO);
            ReflectionTestUtils.setField(request, "applicationId", null);

            ConsentCreateRequest.ConsentItem item = new ConsentCreateRequest.ConsentItem();
            ReflectionTestUtils.setField(item, "termId", 5L);
            ReflectionTestUtils.setField(item, "isConsented", true);

            ReflectionTestUtils.setField(request, "consents", List.of(item));

            // when
            List<ConsentHistory> result = TermConverter.toConsentHistoryList(user, termMap, null, request.getConsents());

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getApplication()).isNull();
        }
    }

    @Nested
    @DisplayName("toConsentResponse 메서드")
    class ToConsentResponseTest {

        @Test
        @DisplayName("termType, applicationId, userId가 응답에 정확히 매핑된다")
        void 기본_필드가_응답에_정확히_매핑된다() {
            // given
            TermType termType = TermType.MYDATA;
            Long applicationId = 50L;
            Long userId = 3L;

            ConsentHistory history = ConsentHistory.builder()
                    .user(createUser(userId))
                    .term(createTerm(11L, TermType.MYDATA, true))
                    .application(createApplication(applicationId))
                    .isConsented(true)
                    .build();
            ReflectionTestUtils.setField(history, "consentedAt", LocalDateTime.of(2024, 1, 15, 10, 30, 0));

            // when
            ConsentCreateResponse response = TermConverter.toConsentResponse(termType, applicationId, userId,
                    List.of(history));

            // then
            assertThat(response.termType()).isEqualTo(TermType.MYDATA);
            assertThat(response.applicationId()).isEqualTo(50L);
            assertThat(response.userId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("각 ConsentHistory가 ConsentItemResponse로 정확히 변환된다")
        void ConsentHistory가_ConsentItemResponse로_정확히_변환된다() {
            // given
            TermType termType = TermType.MYBIZDATA;
            Long applicationId = null;
            Long userId = 4L;

            LocalDateTime consentedAt1 = LocalDateTime.of(2024, 3, 10, 9, 0, 0);
            LocalDateTime consentedAt2 = LocalDateTime.of(2024, 3, 10, 9, 0, 1);

            ConsentHistory history1 = ConsentHistory.builder()
                    .user(createUser(userId))
                    .term(createTerm(21L, TermType.MYBIZDATA, true))
                    .application(null)
                    .isConsented(true)
                    .build();
            ReflectionTestUtils.setField(history1, "consentedAt", consentedAt1);

            ConsentHistory history2 = ConsentHistory.builder()
                    .user(createUser(userId))
                    .term(createTerm(22L, TermType.MYBIZDATA, false))
                    .application(null)
                    .isConsented(false)
                    .build();
            ReflectionTestUtils.setField(history2, "consentedAt", consentedAt2);

            // when
            ConsentCreateResponse response = TermConverter.toConsentResponse(termType, applicationId, userId,
                    List.of(history1, history2));

            // then
            assertThat(response.consents()).hasSize(2);

            ConsentItemResponse firstItem = response.consents().get(0);
            assertThat(firstItem.termId()).isEqualTo(21L);
            assertThat(firstItem.isConsented()).isTrue();
            assertThat(firstItem.consentedAt()).isEqualTo(consentedAt1);

            ConsentItemResponse secondItem = response.consents().get(1);
            assertThat(secondItem.termId()).isEqualTo(22L);
            assertThat(secondItem.isConsented()).isFalse();
            assertThat(secondItem.consentedAt()).isEqualTo(consentedAt2);
        }
    }

    // === Helper Methods ===

    private User createUser(Long userId) {
        try {
            var constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            User user = constructor.newInstance();
            ReflectionTestUtils.setField(user, "userId", userId);
            return user;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Term createTerm(Long termId, TermType termType, Boolean isRequired) {
        try {
            var constructor = Term.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Term term = constructor.newInstance();
            ReflectionTestUtils.setField(term, "termId", termId);
            ReflectionTestUtils.setField(term, "termType", termType);
            ReflectionTestUtils.setField(term, "isRequired", isRequired);
            return term;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Term createTermFull(Long termId, TermType termType, String version, String title,
                                String fileUrl, Boolean isRequired, LocalDateTime effectiveAt) {
        try {
            var constructor = Term.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Term term = constructor.newInstance();
            ReflectionTestUtils.setField(term, "termId", termId);
            ReflectionTestUtils.setField(term, "termType", termType);
            ReflectionTestUtils.setField(term, "version", version);
            ReflectionTestUtils.setField(term, "title", title);
            ReflectionTestUtils.setField(term, "fileUrl", fileUrl);
            ReflectionTestUtils.setField(term, "isRequired", isRequired);
            ReflectionTestUtils.setField(term, "effectiveAt", effectiveAt);
            return term;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LoanApplication createApplication(Long applicationId) {
        try {
            var constructor = LoanApplication.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            LoanApplication application = constructor.newInstance();
            ReflectionTestUtils.setField(application, "applicationId", applicationId);
            return application;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
