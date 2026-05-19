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
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.LoanDecisionRepository;
import com.sofit.common.repository.LoanExecutionRepository;
import com.sofit.user.domain.loan.client.CodefClient;
import com.sofit.user.domain.loan.converter.LoanExecutionConverter;
import com.sofit.user.domain.loan.dto.request.AccountVerificationConfirmRequest;
import com.sofit.user.domain.loan.dto.request.AccountVerificationRequest;
import com.sofit.user.domain.loan.dto.response.AccountVerificationConfirmResponse;
import com.sofit.user.domain.loan.dto.response.AccountVerificationResponse;
import com.sofit.user.domain.loan.dto.response.LoanExecutionResultResponse;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.loan.util.AccountMaskingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanExecutionServiceImpl implements LoanExecutionService {

    private static final String VERIFICATION_KEY_PREFIX = "verification:";
    private static final long VERIFICATION_TTL_SECONDS = 300; // 5분
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // 등록된 은행 기관코드 목록
    private static final List<String> VALID_BANK_CODES = List.of(
            "0002", "0003", "0004", "0007", "0011", "0012", "0020", "0023",
            "0027", "0031", "0032", "0034", "0035", "0037", "0039", "0045",
            "0048", "0071", "0081", "0088", "0089", "0090", "0092"
    );

    private final LoanExecutionRepository loanExecutionRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanDecisionRepository loanDecisionRepository;
    private final CodefClient codefClient;
    private final AccountVerificationRateLimiter rateLimiter;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoanExecutionResultResponse findExecutionResult(Long userId, Long applicationId) {
        LoanExecution execution = loanExecutionRepository
                .findByApplicationIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.EXECUTION_NOT_FOUND));

        LoanDecision decision = loanDecisionRepository
                .findByApplication_ApplicationId(applicationId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.LOAN_DECISION_NOT_FOUND));

        return LoanExecutionConverter.toResponse(execution, decision);
    }

    @Override
    public AccountVerificationResponse requestAccountVerification(Long applicationId, AccountVerificationRequest request) {
        String bankCode = request.getBankCode();
        String accountNumber = request.getAccountNumber();

        // 은행코드 유효성 검증
        if (!VALID_BANK_CODES.contains(bankCode)) {
            throw new BaseException(LoanErrorCode.ACCOUNT_INVALID_BANK_CODE);
        }

        // 계좌번호 9자리 미만 마스킹 불가 → 거부
        if (accountNumber.length() < 9) {
            throw new BaseException(LoanErrorCode.ACCOUNT_INVALID);
        }

        // Rate Limit 확인
        if (!rateLimiter.isAllowed(accountNumber)) {
            throw new BaseException(LoanErrorCode.ACCOUNT_RATE_LIMIT_EXCEEDED);
        }

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

        // Rate Limiter 카운터 증가 (코데프 API 성공 후에만)
        rateLimiter.increment(accountNumber);

        // 응답 생성
        String maskedAccount = AccountMaskingUtil.mask(accountNumber);
        String bankName = getBankName(bankCode);
        String expiredAt = LocalDateTime.now(KST).plusMinutes(5).format(ISO_FORMAT);

        return new AccountVerificationResponse(bankName, maskedAccount, "", expiredAt);
    }

    @Override
    @Transactional
    public AccountVerificationConfirmResponse confirmAccountVerification(Long applicationId, AccountVerificationConfirmRequest request) {
        String redisKey = VERIFICATION_KEY_PREFIX + applicationId;

        // Redis에서 인증 정보 조회
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(redisKey);
        if (entries.isEmpty()) {
            throw new BaseException(LoanErrorCode.ACCOUNT_VERIFICATION_EXPIRED);
        }

        String storedAuthCode = (String) entries.get("authCode");
        String bankCode = (String) entries.get("bankCode");
        String accountNumber = (String) entries.get("accountNumber");

        // 인증코드 비교
        if (!request.getVerificationCode().equals(storedAuthCode)) {
            throw new BaseException(LoanErrorCode.ACCOUNT_VERIFICATION_MISMATCH);
        }

        // 인증 성공 → Redis 삭제 (재사용 방지)
        redisTemplate.delete(redisKey);

        // LoanApplication 조회 후 LoanExecution INSERT
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        LoanExecution execution = new LoanExecution(application, 0L, accountNumber, bankCode);
        loanExecutionRepository.save(execution);

        return new AccountVerificationConfirmResponse(true);
    }

    /**
     * 은행코드 → 은행명 변환
     */
    private String getBankName(String bankCode) {
        return switch (bankCode) {
            case "0002" -> "산업은행";
            case "0003" -> "기업은행";
            case "0004" -> "국민은행";
            case "0007" -> "수협은행";
            case "0011" -> "농협은행";
            case "0012" -> "지역농축협";
            case "0020" -> "우리은행";
            case "0023" -> "SC제일은행";
            case "0027" -> "한국씨티은행";
            case "0031" -> "대구은행";
            case "0032" -> "부산은행";
            case "0034" -> "광주은행";
            case "0035" -> "제주은행";
            case "0037" -> "전북은행";
            case "0039" -> "경남은행";
            case "0045" -> "새마을금고";
            case "0048" -> "신협";
            case "0071" -> "우체국";
            case "0081" -> "하나은행";
            case "0088" -> "신한은행";
            case "0089" -> "케이뱅크";
            case "0090" -> "카카오뱅크";
            case "0092" -> "토스뱅크";
            default -> "기타은행";
        };
    }
}
