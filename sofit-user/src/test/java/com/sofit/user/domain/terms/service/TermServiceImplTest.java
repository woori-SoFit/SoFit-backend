package com.sofit.user.domain.terms.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.common.repository.ConsentHistoryRepository;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.TermRepository;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.exception.TermErrorCode;

import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
class TermServiceImplTest {

    @Mock
    private TermRepository termRepository;

    @Mock
    private ConsentHistoryRepository consentHistoryRepository;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @InjectMocks
    private TermServiceImpl termService;

    @Test
    @DisplayName("세션에 userId가 없으면 UNAUTHORIZED 예외가 발생한다")
    void 세션_userId_없으면_UNAUTHORIZED_예외_발생() {
        // given
        HttpSession session = mock(HttpSession.class);
        given(session.getAttribute("userId")).willReturn(null);

        ConsentCreateRequest request = createRequest(TermType.PERSONAL_INFO, null, List.of(
                createConsentItem(1L, true)
        ));

        // when & then
        assertThatThrownBy(() -> termService.createConsents(session, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(GeneralErrorCode.UNAUTHORIZED);
                });
    }

    @Test
    @DisplayName("존재하지 않는 termId로 요청 시 TERM_NOT_FOUND 예외가 발생한다")
    void 존재하지_않는_termId_요청시_TERM_NOT_FOUND_예외_발생() {
        // given
        HttpSession session = sessionOf(1L);
        ConsentCreateRequest request = createRequest(TermType.PERSONAL_INFO, null, List.of(
                createConsentItem(100L, true),
                createConsentItem(200L, true)
        ));

        Term term = createTerm(100L, TermType.PERSONAL_INFO, true);
        given(termRepository.findAllById(List.of(100L, 200L))).willReturn(List.of(term));

        // when & then
        assertThatThrownBy(() -> termService.createConsents(session, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(TermErrorCode.TERM_NOT_FOUND);
                });

        verify(consentHistoryRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("termType이 일치하지 않으면 TERM_TYPE_MISMATCH 예외가 발생한다")
    void termType_불일치시_TERM_TYPE_MISMATCH_예외_발생() {
        // given
        HttpSession session = sessionOf(1L);
        ConsentCreateRequest request = createRequest(TermType.PERSONAL_INFO, null, List.of(
                createConsentItem(1L, true)
        ));

        Term term = createTerm(1L, TermType.MYDATA, true);
        given(termRepository.findAllById(List.of(1L))).willReturn(List.of(term));

        // when & then
        assertThatThrownBy(() -> termService.createConsents(session, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(TermErrorCode.TERM_TYPE_MISMATCH);
                });

        verify(consentHistoryRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("필수 약관에 동의하지 않으면 REQUIRED_TERM_NOT_CONSENTED 예외가 발생한다")
    void 필수_약관_미동의시_REQUIRED_TERM_NOT_CONSENTED_예외_발생() {
        // given
        HttpSession session = sessionOf(1L);
        ConsentCreateRequest request = createRequest(TermType.PERSONAL_INFO, null, List.of(
                createConsentItem(1L, false)
        ));

        Term requiredTerm = createTerm(1L, TermType.PERSONAL_INFO, true);
        given(termRepository.findAllById(List.of(1L))).willReturn(List.of(requiredTerm));

        // when & then
        assertThatThrownBy(() -> termService.createConsents(session, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(TermErrorCode.REQUIRED_TERM_NOT_CONSENTED);
                });

        verify(consentHistoryRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("applicationId 소유권 검증 실패 시 APPLICATION_NOT_FOUND 예외가 발생한다")
    void applicationId_소유권_검증_실패시_예외_발생() {
        // given
        Long userId = 1L;
        Long applicationId = 99L;
        HttpSession session = sessionOf(userId);
        ConsentCreateRequest request = createRequest(TermType.LOAN_APPLICATION, applicationId, List.of(
                createConsentItem(1L, true)
        ));

        Term term = createTerm(1L, TermType.LOAN_APPLICATION, true);
        given(termRepository.findAllById(List.of(1L))).willReturn(List.of(term));
        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(applicationId, userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> termService.createConsents(session, request))
                .isInstanceOf(BaseException.class)
                .satisfies(ex -> {
                    BaseException baseEx = (BaseException) ex;
                    assertThat(baseEx.getErrorCode()).isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
                });

        verify(consentHistoryRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("applicationId가 null이면 소유권 검증을 건너뛴다")
    void applicationId_null이면_소유권_검증_건너뛰기() {
        // given
        Long userId = 1L;
        HttpSession session = sessionOf(userId);
        ConsentCreateRequest request = createRequest(TermType.PERSONAL_INFO, null, List.of(
                createConsentItem(1L, true)
        ));

        Term term = createTerm(1L, TermType.PERSONAL_INFO, true);
        given(termRepository.findAllById(List.of(1L))).willReturn(List.of(term));

        List<ConsentHistory> savedHistories = List.of(
                createConsentHistory(1L, userId, 1L, null, true)
        );
        given(consentHistoryRepository.saveAll(anyList())).willReturn(savedHistories);

        // when
        ConsentCreateResponse response = termService.createConsents(session, request);

        // then
        verify(loanApplicationRepository, never()).findByApplicationIdAndUser_UserId(any(), any());
        assertThat(response).isNotNull();
        assertThat(response.applicationId()).isNull();
    }

    @Test
    @DisplayName("모든 검증 통과 시 saveAll이 호출되고 응답이 정상 반환된다")
    void 모든_검증_통과시_saveAll_호출_및_응답_반환() {
        // given
        Long userId = 1L;
        Long applicationId = 10L;
        HttpSession session = sessionOf(userId);
        ConsentCreateRequest request = createRequest(TermType.LOAN_APPLICATION, applicationId, List.of(
                createConsentItem(1L, true),
                createConsentItem(2L, true)
        ));

        Term requiredTerm = createTerm(1L, TermType.LOAN_APPLICATION, true);
        Term optionalTerm = createTerm(2L, TermType.LOAN_APPLICATION, false);
        given(termRepository.findAllById(List.of(1L, 2L))).willReturn(List.of(requiredTerm, optionalTerm));

        given(loanApplicationRepository.findByApplicationIdAndUser_UserId(applicationId, userId))
                .willReturn(Optional.of(mock(LoanApplication.class)));

        LocalDateTime now = LocalDateTime.now();
        List<ConsentHistory> savedHistories = List.of(
                createConsentHistory(1L, userId, 1L, applicationId, true),
                createConsentHistory(2L, userId, 2L, applicationId, true)
        );
        ReflectionTestUtils.setField(savedHistories.get(0), "consentedAt", now);
        ReflectionTestUtils.setField(savedHistories.get(1), "consentedAt", now);

        given(consentHistoryRepository.saveAll(anyList())).willReturn(savedHistories);

        // when
        ConsentCreateResponse response = termService.createConsents(session, request);

        // then
        verify(consentHistoryRepository).saveAll(anyList());
        assertThat(response).isNotNull();
        assertThat(response.termType()).isEqualTo(TermType.LOAN_APPLICATION);
        assertThat(response.applicationId()).isEqualTo(applicationId);
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.consents()).hasSize(2);
        assertThat(response.consents().get(0).termId()).isEqualTo(1L);
        assertThat(response.consents().get(0).isConsented()).isTrue();
        assertThat(response.consents().get(0).consentedAt()).isEqualTo(now);
    }

    // === Helper Methods ===

    private HttpSession sessionOf(Long userId) {
        HttpSession session = mock(HttpSession.class);
        given(session.getAttribute("userId")).willReturn(userId);
        return session;
    }

    private Term createTerm(Long termId, TermType termType, Boolean isRequired) {
        Term term;
        try {
            var constructor = Term.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            term = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        ReflectionTestUtils.setField(term, "termId", termId);
        ReflectionTestUtils.setField(term, "termType", termType);
        ReflectionTestUtils.setField(term, "isRequired", isRequired);
        ReflectionTestUtils.setField(term, "isActive", true);
        return term;
    }

    private ConsentHistory createConsentHistory(Long consentId, Long userId, Long termId,
                                                 Long applicationId, Boolean isConsented) {
        ConsentHistory history = ConsentHistory.builder()
                .userId(userId)
                .termId(termId)
                .applicationId(applicationId)
                .isConsented(isConsented)
                .build();
        ReflectionTestUtils.setField(history, "consentId", consentId);
        return history;
    }

    private ConsentCreateRequest createRequest(TermType termType, Long applicationId,
                                                List<ConsentCreateRequest.ConsentItem> consents) {
        ConsentCreateRequest request = new ConsentCreateRequest();
        ReflectionTestUtils.setField(request, "termType", termType);
        ReflectionTestUtils.setField(request, "applicationId", applicationId);
        ReflectionTestUtils.setField(request, "consents", consents);
        return request;
    }

    private ConsentCreateRequest.ConsentItem createConsentItem(Long termId, Boolean isConsented) {
        ConsentCreateRequest.ConsentItem item = new ConsentCreateRequest.ConsentItem();
        ReflectionTestUtils.setField(item, "termId", termId);
        ReflectionTestUtils.setField(item, "isConsented", isConsented);
        return item;
    }
}
