package com.sofit.user.domain.terms.service;

import com.sofit.common.entity.term.enums.TermType;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.dto.response.TermListResponse;

public interface TermService {

    TermListResponse findTerms(TermType termType);

    ConsentCreateResponse createConsents(Long userId, ConsentCreateRequest request);
}
