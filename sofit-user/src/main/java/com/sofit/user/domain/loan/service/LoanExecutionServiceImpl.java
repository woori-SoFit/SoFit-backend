package com.sofit.user.domain.loan.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.LoanExecution;
import com.sofit.common.entity.loan.LoanDecision;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.DecisionStatus;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.loan.LoanDecisionRepository;
import com.sofit.common.repository.loan.LoanExecutionRepository;
import com.sofit.user.domain.loan.client.CodefClient;
import com.sofit.user.domain.loan.converter.LoanExecutionConverter;
import com.sofit.user.domain.loan.dto.request.AccountVerificationConfirmRequest;
import com.sofit.user.domain.loan.dto.request.AccountVerificationRequest;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionListResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.loan.util.AccountMaskingUtil;
import com.sofit.user.domain.notification.event.LoanExecutedEvent;
import com.sofit.common.audit.AuditLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanExecutionServiceImpl implements LoanExecutionService {
    private static final String WOORI_BANK_CODE = "0020";

    private static final String VERIFICATION_KEY_PREFIX = "verification:";
    private static final long VERIFICATION_TTL_SECONDS = 300; // 5분
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final LoanExecutionRepository loanExecutionRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDecisionRepository loanDecisionRepository;
    private final CodefClient codefClient;
    private final AccountVerificationRateLimiter rateLimiter;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public LoanExecutionResultResponse findExecutionResult(Long userId, Long applicationId) {
        LoanExecution execution = loanExecutionRepository
                .findByApplicationIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.EXECUTION_NOT_FOUND));

        LoanDecision decision = loanDecisionRepository
                .findByApplication_ApplicationIdAndStatus(applicationId, DecisionStatus.MANAGER_APPROVED)
                .orElseThrow(() -> new BaseException(LoanErrorCode.LOAN_DECISION_NOT_FOUND));

        return LoanExecutionConverter.toResponse(execution, decision);
    }

    @Override
    public LoanExecutionListResponse findExecutionList(Long userId) {
        List<LoanExecution> executions = loanExecutionRepository.findAllByUserId(userId);

        if (executions.isEmpty()) {
            return new LoanExecutionListResponse(List.of());
        }

        List<Long> applicationIds = executions.stream()
                .map(e -> e.getApplication().getApplicationId())
                .toList();

        List<LoanDecision> decisions = loanDecisionRepository
                .findByApplication_ApplicationIdInAndStatusInOrderByCreatedAtAsc(
                        applicationIds, List.of(DecisionStatus.MANAGER_APPROVED));

        return LoanExecutionConverter.toListResponse(executions, decisions);
    }

    @Override
    public AccountVerificationResponse requestAccountVerification(Long userId, Long applicationId, AccountVerificationRequest request) {
        // applicationId 유효성 + APPROVED 상태 + 본인 소유 검증
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        if (!application.getUser().getUserId().equals(userId)) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_OWNED);
        }

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_APPROVED);
        }

        String bankCode = WOORI_BANK_CODE;
        String accountNumber = request.getAccountNumber();

        // 계좌번호 9자리 미만 마스킹 불가 → 거부
        if (accountNumber.length() < 9) {
            throw new BaseException(LoanErrorCode.ACCOUNT_INVALID);
        }

        // Rate Limit 확인 + 카운터 증가 (원자적)
        rateLimiter.checkAndIncrement(accountNumber);

        // 코데프 API 호출
        String authCode = codefClient.requestOneWonTransfer(bankCode, accountNumber);

        // Redis Hash에 인증 정보 저장 (TTL 300초)
        String redisKey = VERIFICATION_KEY_PREFIX + applicationId;
        Map<String, String> verificationData = Map.of(
                "authCode", authCode,
                "bankCode", bankCode,
                "accountNumber", accountNumber
        );
        redisTemplate.opsForHash().putAll(redisKey, verificationData);
        redisTemplate.expire(redisKey, VERIFICATION_TTL_SECONDS, TimeUnit.SECONDS);

        // 응답 생성
        String maskedAccount = AccountMaskingUtil.mask(accountNumber);
        String expiredAt = LocalDateTime.now(KST).plusMinutes(5).format(ISO_FORMAT);

        return LoanExecutionConverter.toVerificationResponse(maskedAccount, authCode, expiredAt);
    }

    @Override
    @Transactional
    @AuditLog(action = "LOAN_ACCOUNT_CONFIRM", target = "계좌 인증 확정 및 대출 실행")
    public AccountVerificationConfirmResponse confirmAccountVerification(Long userId, Long applicationId, AccountVerificationConfirmRequest request) {
        String redisKey = VERIFICATION_KEY_PREFIX + applicationId;

        // Redis에서 인증 정보 조회
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
        if (entries.isEmpty()) {
            throw new BaseException(LoanErrorCode.ACCOUNT_VERIFICATION_EXPIRED);
        }

        String storedAuthCode = (String) entries.get("authCode");
        String accountNumber = (String) entries.get("accountNumber");

        // 인증코드 비교 (코데프 응답: "SOFIT213", 사용자 입력: "213")
        String userInput = "SOFIT" + request.getVerificationCode();
        if (!userInput.equals(storedAuthCode)) {
            throw new BaseException(LoanErrorCode.ACCOUNT_VERIFICATION_MISMATCH);
        }

        // 중복 실행 방지
        if (loanExecutionRepository.findByApplicationId(applicationId).isPresent()) {
            throw new BaseException(LoanErrorCode.EXECUTION_ALREADY_EXISTS);
        }

        // LoanDecision 조회 → 승인 금액 기반으로 LoanExecution 생성
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        if (!application.getUser().getUserId().equals(userId)) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_OWNED);
        }

        if (application.getStatus() != ApplicationStatus.APPROVED) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_APPROVED);
        }

        LoanDecision decision = loanDecisionRepository.findByApplication_ApplicationIdAndStatus(applicationId, DecisionStatus.MANAGER_APPROVED)
                .orElseThrow(() -> new BaseException(LoanErrorCode.LOAN_DECISION_NOT_FOUND));

        LoanExecution execution = new LoanExecution(
                application,
                decision.getApprovedAmount(),
                accountNumber
        );
        loanExecutionRepository.save(execution);

        // 대출 신청 상태를 EXECUTED로 변경
        application.updateStatus(ApplicationStatus.EXECUTED);
        log.info("대출 실행 완료 applicationId={} userId={}", applicationId, userId);

        // DB 저장 성공 후 Redis 삭제 (재사용 방지)
        redisTemplate.delete(redisKey);

        // 대출 실행 완료 알림 이벤트 발행 (트랜잭션 커밋 후 처리)
        // AFTER_COMMIT 이후 영속 컨텍스트가 닫히므로 엔티티 대신 ID만 전달
        eventPublisher.publishEvent(new LoanExecutedEvent(
                application.getUser().getUserId(),
                application.getApplicationId()
        ));

        return LoanExecutionConverter.toVerificationConfirmResponse(true);
    }
}
