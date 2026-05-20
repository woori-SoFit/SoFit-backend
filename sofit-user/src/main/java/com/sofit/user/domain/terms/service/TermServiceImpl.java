package com.sofit.user.domain.terms.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.common.repository.ConsentHistoryRepository;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.TermRepository;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.terms.converter.TermConverter;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest.ConsentItem;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.dto.response.TermListResponse;
import com.sofit.user.domain.terms.exception.TermErrorCode;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;
    private final ConsentHistoryRepository consentHistoryRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    @Value("${sofit.storage.base-url}")
    private String storageBaseUrl;

    @Override
    public TermListResponse findTerms(TermType termType) {
        List<Term> terms = termRepository.findByTermTypeAndIsActiveTrue(termType);
        return TermConverter.toListResponse(terms, storageBaseUrl);
    }

    @Override
    @Transactional
    public ConsentCreateResponse createConsents(HttpSession session, ConsentCreateRequest request) {
        // 1. 세션에서 userId 추출
        Object userIdAttr = session.getAttribute("userId");
        if (userIdAttr == null) {
            throw new BaseException(GeneralErrorCode.UNAUTHORIZED);
        }
        Long userId = (userIdAttr instanceof Long) ? (Long) userIdAttr : Long.valueOf(userIdAttr.toString());

        List<Long> termIds = request.getConsents().stream()
                .map(ConsentItem::getTermId)
                .toList();

        // 2. 약관 존재 여부 검증
        List<Term> foundTerms = termRepository.findAllById(termIds);
        if (foundTerms.size() != termIds.size()) {
            throw new BaseException(TermErrorCode.TERM_NOT_FOUND);
        }

        // 3. termType 일치 검증
        boolean hasTypeMismatch = foundTerms.stream()
                .anyMatch(term -> !term.getTermType().equals(request.getTermType()));
        if (hasTypeMismatch) {
            throw new BaseException(TermErrorCode.TERM_TYPE_MISMATCH);
        }

        // 4. 필수 약관 동의 검증
        Map<Long, Boolean> consentMap = request.getConsents().stream()
                .collect(Collectors.toMap(ConsentItem::getTermId, ConsentItem::getIsConsented));

        boolean hasRequiredNotConsented = foundTerms.stream()
                .filter(term -> Boolean.TRUE.equals(term.getIsRequired()))
                .anyMatch(term -> !Boolean.TRUE.equals(consentMap.get(term.getTermId())));
        if (hasRequiredNotConsented) {
            throw new BaseException(TermErrorCode.REQUIRED_TERM_NOT_CONSENTED);
        }

        // 5. applicationId 소유권 검증 (nullable)
        Long applicationId = request.getApplicationId();
        if (applicationId != null) {
            loanApplicationRepository.findByApplicationIdAndUser_UserId(applicationId, userId)
                    .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));
        }

        // 6. 중복 동의 검증
        boolean hasDuplicate = termIds.stream()
                .anyMatch(termId -> consentHistoryRepository
                        .existsByUserIdAndTermIdAndApplicationId(userId, termId, applicationId));
        if (hasDuplicate) {
            throw new BaseException(TermErrorCode.ALREADY_CONSENTED);
        }

        // 7. ConsentHistory 일괄 저장
        List<ConsentHistory> consentHistories = TermConverter.toConsentHistoryList(userId, request, applicationId);
        List<ConsentHistory> savedHistories = consentHistoryRepository.saveAll(consentHistories);

        // 8. 응답 변환
        return TermConverter.toConsentResponse(request.getTermType(), applicationId, userId, savedHistories);
    }
}
