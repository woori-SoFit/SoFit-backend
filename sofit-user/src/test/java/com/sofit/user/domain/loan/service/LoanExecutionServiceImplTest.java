package com.sofit.user.domain.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.LoanExecution;
import com.sofit.common.entity.loan.LoanProduct;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.loan.LoanExecutionRepository;
import com.sofit.user.domain.loan.client.CodefClient;
import com.sofit.user.domain.loan.dto.request.AccountVerificationConfirmRequest;
import com.sofit.user.domain.loan.dto.request.AccountVerificationRequest;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionListResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.notification.event.LoanExecutedEvent;

@ExtendWith(MockitoExtension.class)
class LoanExecutionServiceImplTest {

    @InjectMocks
    private LoanExecutionServiceImpl loanExecutionService;

    @Mock
    private LoanExecutionRepository loanExecutionRepository;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LoanDecisionRepository loanDecisionRepository;

    @Mock
    private CodefClient codefClient;

    @Mock
    private AccountVerificationRateLimiter rateLimiter;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long APPLICATION_ID = 100L;
    private static final String ACCOUNT_NUMBER = "1234567890";

    // ===== findExecutionList =====

    @Test
    @DisplayName("대출 실행 완료 목록 조회 성공 시 목록을 반환한다")
    void findExecutionList_success() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.EXECUTED);
        LoanExecution execution = new LoanExecution(application, 9_000_000L, ACCOUNT_NUMBER);
        LoanDecision decision = createDecisionWithApplication(application);

        given(loanExecutionRepository.findAllByUserId(USER_ID))
                .willReturn(List.of(execution));
        given(loanDecisionRepository.findByApplication_ApplicationIdInAndStatusInOrderByCreatedAtAsc(
                List.of(APPLICATION_ID), List.of(DecisionStatus.MANAGER_APPROVED)))
                .willReturn(List.of(decision));

        // when
        LoanExecutionListResponse response = loanExecutionService.findExecutionList(USER_ID);

        // then
        assertThat(response.executions()).hasSize(1);
        assertThat(response.executions().get(0).applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.executions().get(0).executedAmount()).isEqualTo(9_000_000L);
    }

    @Test
    @DisplayName("대출 실행 완료 건이 없으면 빈 목록을 반환한다")
    void findExecutionList_empty() {
        // given
        given(loanExecutionRepository.findAllByUserId(USER_ID))
                .willReturn(List.of());

        // when
        LoanExecutionListResponse response = loanExecutionService.findExecutionList(USER_ID);

        // then
        assertThat(response.executions()).isEmpty();
    }

    // ===== findExecutionResult =====

    @Test
    @DisplayName("대출 실행 결과 조회 성공 시 응답을 반환한다")
    void findExecutionResult_success() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.EXECUTED);
        LoanExecution execution = new LoanExecution(application, 9_000_000L, ACCOUNT_NUMBER);
        LoanDecision decision = createDecision();
        given(loanExecutionRepository.findByApplicationIdAndUserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(execution));
        given(loanDecisionRepository.findByApplication_ApplicationIdAndStatus(APPLICATION_ID, DecisionStatus.MANAGER_APPROVED))
                .willReturn(Optional.of(decision));

        // when
        LoanExecutionResultResponse response = loanExecutionService.findExecutionResult(USER_ID, APPLICATION_ID);

        // then
        assertThat(response.applicationId()).isEqualTo(APPLICATION_ID);
        assertThat(response.executedAmount()).isEqualTo(9_000_000L);
    }

    @Test
    @DisplayName("실행 건이 없으면 EXECUTION_NOT_FOUND 예외를 던진다")
    void findExecutionResult_executionNotFound_throws() {
        // given
        given(loanExecutionRepository.findByApplicationIdAndUserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanExecutionService.findExecutionResult(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.EXECUTION_NOT_FOUND);
    }

    @Test
    @DisplayName("실행 결과 조회 시 결정 정보가 없으면 LOAN_DECISION_NOT_FOUND 예외를 던진다")
    void findExecutionResult_decisionNotFound_throws() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.EXECUTED);
        LoanExecution execution = new LoanExecution(application, 9_000_000L, ACCOUNT_NUMBER);
        given(loanExecutionRepository.findByApplicationIdAndUserId(APPLICATION_ID, USER_ID))
                .willReturn(Optional.of(execution));
        given(loanDecisionRepository.findByApplication_ApplicationIdAndStatus(APPLICATION_ID, DecisionStatus.MANAGER_APPROVED))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanExecutionService.findExecutionResult(USER_ID, APPLICATION_ID))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.LOAN_DECISION_NOT_FOUND);
    }

    // ===== requestAccountVerification =====

    @Test
    @DisplayName("계좌 인증 요청 성공 시 마스킹 계좌와 인증코드를 반환하고 Redis에 저장한다")
    void requestAccountVerification_success() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.APPROVED);
        AccountVerificationRequest request = createVerificationRequest(ACCOUNT_NUMBER);
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));
        given(codefClient.requestOneWonTransfer(anyString(), eq(ACCOUNT_NUMBER))).willReturn("SOFIT213");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);

        // when
        AccountVerificationResponse response = loanExecutionService
                .requestAccountVerification(USER_ID, APPLICATION_ID, request);

        // then
        assertThat(response.maskedAccountNumber()).isEqualTo("1234-****-90");
        assertThat(response.authCode()).isEqualTo("SOFIT213");
        verify(rateLimiter).checkAndIncrement(ACCOUNT_NUMBER);
        verify(hashOperations).putAll(anyString(), anyMap());
        verify(redisTemplate).expire(anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("계좌 인증 요청 시 신청 건이 없으면 APPLICATION_NOT_FOUND 예외를 던진다")
    void requestAccountVerification_applicationNotFound_throws() {
        // given
        AccountVerificationRequest request = createVerificationRequest(ACCOUNT_NUMBER);
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanExecutionService.requestAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("계좌 인증 요청 시 본인 소유가 아니면 APPLICATION_NOT_OWNED 예외를 던진다")
    void requestAccountVerification_notOwned_throws() {
        // given
        LoanApplication application = createApplication(OTHER_USER_ID, ApplicationStatus.APPROVED);
        AccountVerificationRequest request = createVerificationRequest(ACCOUNT_NUMBER);
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanExecutionService.requestAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.APPLICATION_NOT_OWNED);
    }

    @Test
    @DisplayName("계좌 인증 요청 시 승인 상태가 아니면 APPLICATION_NOT_APPROVED 예외를 던진다")
    void requestAccountVerification_notApproved_throws() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.SUBMITTED);
        AccountVerificationRequest request = createVerificationRequest(ACCOUNT_NUMBER);
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanExecutionService.requestAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.APPLICATION_NOT_APPROVED);
    }

    @Test
    @DisplayName("계좌번호가 9자리 미만이면 ACCOUNT_INVALID 예외를 던지고 코데프를 호출하지 않는다")
    void requestAccountVerification_shortAccount_throws() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.APPROVED);
        AccountVerificationRequest request = createVerificationRequest("12345678");
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanExecutionService.requestAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.ACCOUNT_INVALID);
        verify(rateLimiter, never()).checkAndIncrement(anyString());
        verify(codefClient, never()).requestOneWonTransfer(anyString(), anyString());
    }

    // ===== confirmAccountVerification =====

    @Test
    @DisplayName("계좌 인증 확인 성공 시 실행 정보를 저장하고 Redis 키를 삭제한다")
    void confirmAccountVerification_success() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.APPROVED);
        LoanDecision decision = createDecision();
        AccountVerificationConfirmRequest request = createConfirmRequest("213");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(anyString())).willReturn(Map.of(
                "authCode", "SOFIT213",
                "accountNumber", ACCOUNT_NUMBER
        ));
        given(loanExecutionRepository.findByApplicationId(APPLICATION_ID)).willReturn(Optional.empty());
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));
        given(loanDecisionRepository.findByApplication_ApplicationIdAndStatus(APPLICATION_ID, DecisionStatus.MANAGER_APPROVED))
                .willReturn(Optional.of(decision));

        // when
        AccountVerificationConfirmResponse response = loanExecutionService
                .confirmAccountVerification(USER_ID, APPLICATION_ID, request);

        // then
        assertThat(response.accountVerified()).isTrue();
        verify(loanExecutionRepository).save(any(LoanExecution.class));
        verify(redisTemplate).delete(anyString());
    }

    @Test
    @DisplayName("Redis에 인증 정보가 없으면 ACCOUNT_VERIFICATION_EXPIRED 예외를 던진다")
    void confirmAccountVerification_expired_throws() {
        // given
        AccountVerificationConfirmRequest request = createConfirmRequest("213");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(anyString())).willReturn(Map.of());

        // when & then
        assertThatThrownBy(() -> loanExecutionService.confirmAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.ACCOUNT_VERIFICATION_EXPIRED);
    }

    @Test
    @DisplayName("인증코드가 일치하지 않으면 ACCOUNT_VERIFICATION_MISMATCH 예외를 던진다")
    void confirmAccountVerification_mismatch_throws() {
        // given
        AccountVerificationConfirmRequest request = createConfirmRequest("999");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(anyString())).willReturn(Map.of(
                "authCode", "SOFIT213",
                "accountNumber", ACCOUNT_NUMBER
        ));

        // when & then
        assertThatThrownBy(() -> loanExecutionService.confirmAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.ACCOUNT_VERIFICATION_MISMATCH);
    }

    @Test
    @DisplayName("이미 실행된 건이면 EXECUTION_ALREADY_EXISTS 예외를 던진다")
    void confirmAccountVerification_alreadyExists_throws() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.APPROVED);
        LoanExecution existing = new LoanExecution(application, 9_000_000L, ACCOUNT_NUMBER);
        AccountVerificationConfirmRequest request = createConfirmRequest("213");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(anyString())).willReturn(Map.of(
                "authCode", "SOFIT213",
                "accountNumber", ACCOUNT_NUMBER
        ));
        given(loanExecutionRepository.findByApplicationId(APPLICATION_ID)).willReturn(Optional.of(existing));

        // when & then
        assertThatThrownBy(() -> loanExecutionService.confirmAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.EXECUTION_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("확인 단계에서 본인 소유가 아니면 APPLICATION_NOT_OWNED 예외를 던진다")
    void confirmAccountVerification_notOwned_throws() {
        // given
        LoanApplication application = createApplication(OTHER_USER_ID, ApplicationStatus.APPROVED);
        AccountVerificationConfirmRequest request = createConfirmRequest("213");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(anyString())).willReturn(Map.of(
                "authCode", "SOFIT213",
                "accountNumber", ACCOUNT_NUMBER
        ));
        given(loanExecutionRepository.findByApplicationId(APPLICATION_ID)).willReturn(Optional.empty());
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanExecutionService.confirmAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.APPLICATION_NOT_OWNED);
    }

    @Test
    @DisplayName("확인 단계에서 결정 정보가 없으면 LOAN_DECISION_NOT_FOUND 예외를 던진다")
    void confirmAccountVerification_decisionNotFound_throws() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.APPROVED);
        AccountVerificationConfirmRequest request = createConfirmRequest("213");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(anyString())).willReturn(Map.of(
                "authCode", "SOFIT213",
                "accountNumber", ACCOUNT_NUMBER
        ));
        given(loanExecutionRepository.findByApplicationId(APPLICATION_ID)).willReturn(Optional.empty());
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));
        given(loanDecisionRepository.findByApplication_ApplicationIdAndStatus(APPLICATION_ID, DecisionStatus.MANAGER_APPROVED))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanExecutionService.confirmAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.LOAN_DECISION_NOT_FOUND);
    }

    @Test
    @DisplayName("확인 단계에서 신청 건이 없으면 APPLICATION_NOT_FOUND 예외를 던진다")
    void confirmAccountVerification_applicationNotFound_throws() {
        // given
        AccountVerificationConfirmRequest request = createConfirmRequest("213");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(anyString())).willReturn(Map.of(
                "authCode", "SOFIT213",
                "accountNumber", ACCOUNT_NUMBER
        ));
        given(loanExecutionRepository.findByApplicationId(APPLICATION_ID)).willReturn(Optional.empty());
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> loanExecutionService.confirmAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.APPLICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("확인 단계에서 APPROVED 상태가 아니면 APPLICATION_NOT_APPROVED 예외를 던진다")
    void confirmAccountVerification_notApproved_throws() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.SUBMITTED);
        AccountVerificationConfirmRequest request = createConfirmRequest("213");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(anyString())).willReturn(Map.of(
                "authCode", "SOFIT213",
                "accountNumber", ACCOUNT_NUMBER
        ));
        given(loanExecutionRepository.findByApplicationId(APPLICATION_ID)).willReturn(Optional.empty());
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));

        // when & then
        assertThatThrownBy(() -> loanExecutionService.confirmAccountVerification(USER_ID, APPLICATION_ID, request))
                .isInstanceOf(BaseException.class)
                .extracting("errorCode")
                .isEqualTo(LoanErrorCode.APPLICATION_NOT_APPROVED);
    }

    @Test
    @DisplayName("계좌 인증 확인 성공 시 대출 실행 완료 알림 이벤트를 발행한다")
    void confirmAccountVerification_success_publishesEvent() {
        // given
        LoanApplication application = createApplication(USER_ID, ApplicationStatus.APPROVED);
        LoanDecision decision = createDecision();
        AccountVerificationConfirmRequest request = createConfirmRequest("213");
        given(redisTemplate.opsForHash()).willReturn(hashOperations);
        given(hashOperations.entries(anyString())).willReturn(Map.of(
                "authCode", "SOFIT213",
                "accountNumber", ACCOUNT_NUMBER
        ));
        given(loanExecutionRepository.findByApplicationId(APPLICATION_ID)).willReturn(Optional.empty());
        given(loanApplicationRepository.findById(APPLICATION_ID)).willReturn(Optional.of(application));
        given(loanDecisionRepository.findByApplication_ApplicationIdAndStatus(APPLICATION_ID, DecisionStatus.MANAGER_APPROVED))
                .willReturn(Optional.of(decision));

        // when
        loanExecutionService.confirmAccountVerification(USER_ID, APPLICATION_ID, request);

        // then
        verify(eventPublisher).publishEvent(any(LoanExecutedEvent.class));
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.EXECUTED);
    }

    // ===== helpers =====

    private LoanApplication createApplication(Long userId, ApplicationStatus status) {
        try {
            User user = newInstance(User.class);
            setField(user, "userId", userId);

            LoanProduct product = newInstance(LoanProduct.class);
            setField(product, "productId", 10L);
            setField(product, "productName", "우리 사업자 대출");

            LoanApplication application = newInstance(LoanApplication.class);
            setField(application, "applicationId", APPLICATION_ID);
            setField(application, "user", user);
            setField(application, "product", product);
            setField(application, "status", status);
            return application;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private LoanDecision createDecision() {
        try {
            LoanDecision decision = newInstance(LoanDecision.class);
            setField(decision, "approvedAmount", 9_000_000L);
            setField(decision, "approvedRate", new java.math.BigDecimal("5.50"));
            setField(decision, "approvedTerm", 12);
            return decision;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private LoanDecision createDecisionWithApplication(LoanApplication application) {
        try {
            LoanDecision decision = newInstance(LoanDecision.class);
            setField(decision, "application", application);
            setField(decision, "approvedAmount", 9_000_000L);
            setField(decision, "approvedRate", new java.math.BigDecimal("5.50"));
            setField(decision, "approvedTerm", 12);
            setField(decision, "status", DecisionStatus.MANAGER_APPROVED);
            return decision;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private AccountVerificationRequest createVerificationRequest(String accountNumber) {
        try {
            AccountVerificationRequest request = newInstance(AccountVerificationRequest.class);
            setField(request, "accountNumber", accountNumber);
            return request;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private AccountVerificationConfirmRequest createConfirmRequest(String code) {
        try {
            AccountVerificationConfirmRequest request = newInstance(AccountVerificationConfirmRequest.class);
            setField(request, "verificationCode", code);
            return request;
        } catch (Exception e) {
            throw new RuntimeException("테스트 데이터 생성 실패", e);
        }
    }

    private <T> T newInstance(Class<T> clazz) throws Exception {
        var constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = findField(target.getClass(), fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new RuntimeException("필드를 찾을 수 없습니다: " + fieldName);
    }
}
