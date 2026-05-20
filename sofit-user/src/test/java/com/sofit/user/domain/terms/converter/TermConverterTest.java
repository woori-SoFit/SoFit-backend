package com.sofit.user.domain.terms.converter;

import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse.ConsentItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TermConverterTest {

    @Nested
    @DisplayName("toConsentHistoryList 메서드")
    class ToConsentHistoryListTest {

        @Test
        @DisplayName("요청의 userId, termId, applicationId, isConsented가 정확히 매핑된다")
        void 요청_필드가_ConsentHistory에_정확히_매핑된다() {
            // given
            Long userId = 1L;
            Long applicationId = 100L;

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
            List<ConsentHistory> result = TermConverter.toConsentHistoryList(userId, request, applicationId);

            // then
            assertThat(result).hasSize(2);

            ConsentHistory first = result.get(0);
            assertThat(first.getUserId()).isEqualTo(userId);
            assertThat(first.getTermId()).isEqualTo(10L);
            assertThat(first.getApplicationId()).isEqualTo(applicationId);
            assertThat(first.getIsConsented()).isTrue();

            ConsentHistory second = result.get(1);
            assertThat(second.getUserId()).isEqualTo(userId);
            assertThat(second.getTermId()).isEqualTo(20L);
            assertThat(second.getApplicationId()).isEqualTo(applicationId);
            assertThat(second.getIsConsented()).isFalse();
        }

        @Test
        @DisplayName("applicationId가 null이면 ConsentHistory의 applicationId도 null이다")
        void applicationId가_null이면_null로_매핑된다() {
            // given
            Long userId = 2L;

            ConsentCreateRequest request = new ConsentCreateRequest();
            ReflectionTestUtils.setField(request, "termType", TermType.PERSONAL_INFO);
            ReflectionTestUtils.setField(request, "applicationId", null);

            ConsentCreateRequest.ConsentItem item = new ConsentCreateRequest.ConsentItem();
            ReflectionTestUtils.setField(item, "termId", 5L);
            ReflectionTestUtils.setField(item, "isConsented", true);

            ReflectionTestUtils.setField(request, "consents", List.of(item));

            // when
            List<ConsentHistory> result = TermConverter.toConsentHistoryList(userId, request, null);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getApplicationId()).isNull();
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
                    .userId(userId)
                    .termId(11L)
                    .applicationId(applicationId)
                    .isConsented(true)
                    .build();
            ReflectionTestUtils.setField(history, "consentedAt", LocalDateTime.of(2024, 1, 15, 10, 30, 0));

            List<ConsentHistory> savedHistories = List.of(history);

            // when
            ConsentCreateResponse response = TermConverter.toConsentResponse(termType, applicationId, userId, savedHistories);

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
                    .userId(userId)
                    .termId(21L)
                    .applicationId(applicationId)
                    .isConsented(true)
                    .build();
            ReflectionTestUtils.setField(history1, "consentedAt", consentedAt1);

            ConsentHistory history2 = ConsentHistory.builder()
                    .userId(userId)
                    .termId(22L)
                    .applicationId(applicationId)
                    .isConsented(false)
                    .build();
            ReflectionTestUtils.setField(history2, "consentedAt", consentedAt2);

            List<ConsentHistory> savedHistories = List.of(history1, history2);

            // when
            ConsentCreateResponse response = TermConverter.toConsentResponse(termType, applicationId, userId, savedHistories);

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
}
