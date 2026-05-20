package com.sofit.user.domain.terms.service;

import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.common.repository.TermRepository;
import com.sofit.user.domain.terms.converter.TermConverter;
import com.sofit.user.domain.terms.dto.response.TermListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;

    @Value("${sofit.storage.base-url}")
    private String storageBaseUrl;

    @Override
    public TermListResponse findTerms(TermType termType) {
        List<Term> terms = termRepository.findByTermTypeAndIsActiveTrue(termType);
        return TermConverter.toListResponse(terms, storageBaseUrl);
    }
}
