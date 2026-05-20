package com.sofit.user.domain.terms.converter;

import java.util.List;

import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse.ConsentItemResponse;
import com.sofit.user.domain.terms.dto.response.TermListResponse;

public class TermConverter {

    private TermConverter() {
    }

    public static TermListResponse toListResponse(List<Term> terms, String baseUrl) {
        List<TermListResponse.TermItem> items = terms.stream()
                .map(term -> new TermListResponse.TermItem(
                        term.getTermId(),
                        term.getTermType().name(),
                        term.getVersion(),
                        term.getTitle(),
                        baseUrl + term.getFileUrl(),
                        term.getIsRequired(),
                        term.getEffectiveAt()
                ))
                .toList();

        return new TermListResponse(items);
    }

    public static List<ConsentHistory> toConsentHistoryList(Long userId, ConsentCreateRequest request, Long applicationId) {
        return request.getConsents().stream()
                .map(item -> ConsentHistory.builder()
                        .userId(userId)
                        .termId(item.getTermId())
                        .applicationId(applicationId)
                        .isConsented(item.getIsConsented())
                        .build())
                .toList();
    }

    public static ConsentCreateResponse toConsentResponse(TermType termType, Long applicationId, Long userId,
                                                          List<ConsentHistory> savedHistories) {
        List<ConsentItemResponse> consents = savedHistories.stream()
                .map(history -> new ConsentItemResponse(
                        history.getTermId(),
                        history.getIsConsented(),
                        history.getConsentedAt()))
                .toList();

        return new ConsentCreateResponse(termType, applicationId, userId, consents);
    }
}
