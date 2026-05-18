package com.sofit.user.domain.terms.converter;

import com.sofit.common.entity.term.Term;
import com.sofit.user.domain.terms.dto.response.TermListResponse;

import java.util.List;

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
                        term.getIsActive(),
                        term.getEffectiveAt()
                ))
                .toList();

        return new TermListResponse(items);
    }
}
