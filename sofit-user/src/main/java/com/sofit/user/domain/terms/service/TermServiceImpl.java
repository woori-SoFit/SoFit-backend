package com.sofit.user.domain.terms.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.term.ConsentHistoryRepository;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.term.TermRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.terms.converter.TermConverter;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest.ConsentItem;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.dto.response.TermListResponse;
import com.sofit.user.domain.terms.exception.TermErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermServiceImpl implements TermService {

    private final TermRepository termRepository;
    private final ConsentHistoryRepository consentHistoryRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final UserRepository userRepository;

    @Override
    public TermListResponse findTerms(TermType termType) {
        List<Term> terms = termRepository.findByTermTypeAndIsActiveTrue(termType);
        return TermConverter.toListResponse(terms);
    }

    @Override
    @Transactional
    public ConsentCreateResponse createConsents(Long userId, ConsentCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(AuthErrorCode.USER_NOT_FOUND));

        List<Long> termIds = request.getConsents().stream()
                .map(ConsentItem::getTermId)
                .toList();

        // 2. 약관 존재 및 활성화 여부 검증
        List<Term> foundTerms = termRepository.findAllByTermIdInAndIsActiveTrue(termIds);
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
        LoanApplication application = null;
        if (applicationId != null) {
            application = loanApplicationRepository.findByApplicationIdAndUser_UserId(applicationId, userId)
                    .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));
        }

        // 6. 기존 동의 이력 조회 (이미 전부 동의된 경우 저장 없이 기존 이력 반환)
        Map<Long, Term> termMap = foundTerms.stream()
                .collect(Collectors.toMap(Term::getTermId, t -> t));

        List<ConsentHistory> existingHistories = consentHistoryRepository
                .findExistingConsents(userId, termIds, applicationId);
        if (existingHistories.size() == termIds.size()) {
            return TermConverter.toConsentResponse(request.getTermType(), applicationId, userId, existingHistories);
        }

        // 7. 신규 동의 항목 ConsentHistory 저장
        List<ConsentHistory> savedHistories = consentHistoryRepository.saveAll(
                TermConverter.toConsentHistoryList(user, termMap, application, request.getConsents()));

        // 8. 응답 변환
        return TermConverter.toConsentResponse(request.getTermType(), applicationId, userId, savedHistories);
    }
}
