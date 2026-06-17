package com.sofit.user.domain.auth.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.auth.RegistrationProcess;
import com.sofit.common.entity.auth.enums.RegistrationStep;
import com.sofit.common.entity.sGrade.SGradeHistory;
import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.term.Term;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserStatus;
import com.sofit.common.repository.term.ConsentHistoryRepository;
import com.sofit.common.repository.term.TermRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.auth.RegistrationProcessRepository;
import com.sofit.common.repository.sGrade.SGradeHistoryRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.client.ExternalMockClient;
import com.sofit.user.domain.sgrade.service.SGradeService;
import com.sofit.user.domain.terms.exception.TermErrorCode;
import com.sofit.user.domain.auth.converter.AuthConverter;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.request.FinancialCertVerifyRequest;
import com.sofit.user.domain.auth.dto.request.LoginRequest;
import com.sofit.user.domain.auth.dto.request.SignupCompleteRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.external.ExternalKycResponse;
import com.sofit.user.domain.auth.dto.external.ExternalMockApiResponse;
import com.sofit.user.domain.auth.dto.response.CheckLoginIdResponse;
import com.sofit.user.domain.auth.dto.response.LoginResponse;
import com.sofit.user.domain.auth.dto.response.SignupCompleteResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import com.sofit.common.audit.AuditLog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import com.sofit.common.logging.LogMaskUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final ExternalMockClient externalMockClient;
    private final FinancialCertService financialCertService;
    private final LoginAttemptService loginAttemptService;
    private final UserRepository userRepository;
    private final RegistrationProcessRepository registrationProcessRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final TermRepository termRepository;
    private final ConsentHistoryRepository consentHistoryRepository;
    private final SGradeHistoryRepository sGradeHistoryRepository;
    private final SGradeService sGradeService;
    private final PasswordEncoder passwordEncoder;
    private final HttpSessionSecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final TransactionTemplate transactionTemplate;

    private final String REGISTRATIONPROCESSID = "registrationProcessId";

    private record SignupResult(User user, Long sGradeId) {
    }

    @Override
    public BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request, HttpSession session) {
        // 1. 이미 가입 완료된 사업자 체크 + 기존 프로세스 조회 (트랜잭션)
        RegistrationProcess existingProcess = transactionTemplate.execute(status -> {
            businessProfileRepository.findByBusinessNumber(request.getBusinessNumber())
                    .filter(bp -> bp.getUser().getStatus() == UserStatus.ACTIVE)
                    .ifPresent(bp -> {
                        throw new BaseException(AuthErrorCode.BUSINESS_ALREADY_REGISTERED);
                    });

            return registrationProcessRepository
                    .findByBusinessNumber(request.getBusinessNumber())
                    .orElse(null);
        });

        // 2. 유효한 레코드면 기존 결과 반환 (DB 작업 없음)
        if (existingProcess != null
                && existingProcess.getStep() == RegistrationStep.KYC_VERIFIED
                && existingProcess.getUpdatedAt().plusMinutes(30).isAfter(LocalDateTime.now())) {
            session.setAttribute(REGISTRATIONPROCESSID, existingProcess.getRegistrationProcessId());
            return AuthConverter.toBusinessVerificationResponse(existingProcess);
        }

        // 3. External Mock 호출 (트랜잭션 밖 — DB 커넥션 미점유)
        ExternalMockApiResponse<ExternalKycResponse> mockResponse =
                externalMockClient.callKycVerify(request.getBusinessNumber());

        if (!mockResponse.isSuccess() || mockResponse.result() == null || !mockResponse.result().isValid()) {
            throw new BaseException(AuthErrorCode.BUSINESS_NOT_FOUND);
        }

        ExternalKycResponse kycResult = mockResponse.result();

        // 4. DB 저장 (트랜잭션)
        final RegistrationProcess finalExistingProcess = existingProcess;
        LocalDate openDate = kycResult.openDate() != null && !kycResult.openDate().isBlank()
                ? LocalDate.parse(kycResult.openDate())
                : null;
        RegistrationProcess process = transactionTemplate.execute(status -> {
            if (finalExistingProcess != null && finalExistingProcess.getStep() == RegistrationStep.KYC_VERIFIED) {
                finalExistingProcess.updateKycResult(
                        kycResult.businessNumber(),
                        kycResult.businessName(),
                        kycResult.representativeName(),
                        openDate,
                        kycResult.businessType(),
                        kycResult.businessCategory(),
                        kycResult.businessAddress()
                );
                return registrationProcessRepository.save(finalExistingProcess);
            } else {
                if (finalExistingProcess != null) {
                    registrationProcessRepository.delete(finalExistingProcess);
                    registrationProcessRepository.flush();
                }
                RegistrationProcess newProcess = RegistrationProcess.createForStep1(
                        kycResult.businessNumber(),
                        kycResult.businessName(),
                        kycResult.representativeName(),
                        openDate,
                        kycResult.businessType(),
                        kycResult.businessCategory(),
                        kycResult.businessAddress()
                );
                return registrationProcessRepository.save(newProcess);
            }
        });

        session.setAttribute(REGISTRATIONPROCESSID, process.getRegistrationProcessId());
        return AuthConverter.toBusinessVerificationResponse(kycResult);
    }

    @Override
    @AuditLog(action = "FINANCIAL_CERT_VERIFY", target = "금융인증서 인증")
    public void verifyFinancialCertificate(FinancialCertVerifyRequest request, HttpSession session) {
        // 1. 인증은 FinancialCertService에 위임
        financialCertService.verify(request);

        // 2. 회원가입 플로우인 경우 RegistrationProcess 후처리 (트랜잭션)
        Long processId = (Long) session.getAttribute(REGISTRATIONPROCESSID);
        if (processId != null) {
            processRegistrationStep2(processId);
        }
        log.info("금융인증서 인증 완료");
    }

    /**
     * 금융인증서 검증 성공 후 RegistrationProcess Step 2 처리.processRegistrationStep2
     * 외부 API 호출 이후 DB 작업만 수행하므로 커넥션 점유 시간 최소화.
     * 만료 시에도 EXPIRED 상태가 DB에 반영되도록 만료 저장을 별도 트랜잭션으로 처리.
     */
    private void processRegistrationStep2(Long processId) {
        RegistrationProcess process = transactionTemplate.execute(status -> {
            return registrationProcessRepository.findById(processId)
                    .orElseThrow(() -> {
                        log.warn("[verifyFinancialCertificate] processId={} 세션에 있지만 DB 레코드 없음", processId);
                        return new BaseException(AuthErrorCode.STEP_NOT_COMPLETED);
                    });
        });

        // 만료 체크 — 만료 상태를 별도 트랜잭션으로 저장 후 예외 전파
        if (process.getUpdatedAt().plusMinutes(30).isBefore(LocalDateTime.now())) {
            transactionTemplate.executeWithoutResult(status -> {
                process.expire();
                registrationProcessRepository.save(process);
            });
            throw new BaseException(AuthErrorCode.REGISTRATION_EXPIRED);
        }

        // KYC 인증 완료 여부 확인
        if (process.getStep() == RegistrationStep.PIN_VERIFIED) {
            throw new BaseException(AuthErrorCode.STEP_ALREADY_COMPLETED);
        }
        if (process.getStep() != RegistrationStep.KYC_VERIFIED) {
            throw new BaseException(AuthErrorCode.STEP_NOT_COMPLETED);
        }

        // Step 2 완료 처리
        transactionTemplate.executeWithoutResult(status -> {
            process.completeStep2();
            registrationProcessRepository.save(process);
        });
    }

    @Override
    @AuditLog(action = "SIGNUP", target = "회원가입 완료")
    public SignupCompleteResponse completeSignup(SignupCompleteRequest request, HttpSession session) {
        // 1. 세션에서 registrationProcessId 조회
        Long processId = (Long) session.getAttribute(REGISTRATIONPROCESSID);
        if (processId == null) {
            throw new BaseException(AuthErrorCode.STEP_NOT_COMPLETED);
        }

        RegistrationProcess process = transactionTemplate.execute(status ->
                registrationProcessRepository.findById(processId)
                        .orElseThrow(() -> new BaseException(AuthErrorCode.REGISTRATION_EXPIRED))
        );

        // 2. 만료 체크 — 만료 상태를 별도 트랜잭션으로 저장 후 예외 전파
        if (process.getUpdatedAt().plusMinutes(30).isBefore(LocalDateTime.now())) {
            transactionTemplate.executeWithoutResult(status -> {
                process.expire();
                registrationProcessRepository.save(process);
            });
            throw new BaseException(AuthErrorCode.REGISTRATION_EXPIRED);
        }

        // 3. PIN 인증 완료 여부 확인
        if (process.getStep() != RegistrationStep.PIN_VERIFIED) {
            throw new BaseException(AuthErrorCode.STEP_NOT_COMPLETED);
        }

        // 4. 약관 검증
        List<Long> termIds = request.getConsents().stream()
                .map(SignupCompleteRequest.ConsentItem::getTermId)
                .toList();

        List<Term> terms = termRepository.findAllByTermIdInAndIsActiveTrue(termIds);
        if (terms.size() != termIds.size()) {
            throw new BaseException(TermErrorCode.TERM_NOT_FOUND);
        }

        boolean hasTypeMismatch = terms.stream()
                .anyMatch(term -> !term.getTermType().equals(TermType.PERSONAL_INFO));
        if (hasTypeMismatch) {
            throw new BaseException(TermErrorCode.TERM_TYPE_MISMATCH);
        }

        java.util.Map<Long, Boolean> consentMap = request.getConsents().stream()
                .collect(java.util.stream.Collectors.toMap(
                        SignupCompleteRequest.ConsentItem::getTermId,
                        SignupCompleteRequest.ConsentItem::getIsConsented));

        boolean hasRequiredNotConsented = terms.stream()
                .filter(term -> Boolean.TRUE.equals(term.getIsRequired()))
                .anyMatch(term -> !Boolean.TRUE.equals(consentMap.get(term.getTermId())));
        if (hasRequiredNotConsented) {
            throw new BaseException(TermErrorCode.REQUIRED_TERM_NOT_CONSENTED);
        }

        // 5~9. 가입 처리 (트랜잭션)
        java.util.Map<Long, Term> termMap = terms.stream()
                .collect(java.util.stream.Collectors.toMap(Term::getTermId, t -> t));

        SignupResult signupResult = transactionTemplate.execute(status -> {
            // loginId 중복 체크
            if (userRepository.existsByLoginId(request.getLoginId())) {
                throw new BaseException(AuthErrorCode.LOGIN_ID_DUPLICATED);
            }

            // User 생성
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            User newUser = User.createUser(
                    request.getLoginId(),
                    encodedPassword,
                    request.getName(),
                    request.getPhoneNumber(),
                    request.getResidentNumber()
            );
            userRepository.save(newUser);

            // BusinessProfile 생성 (KYC 데이터 기반)
            BusinessProfile businessProfile = BusinessProfile.createVerified(
                    newUser,
                    process.getBusinessNumber(),
                    process.getRepresentativeName(),
                    process.getBusinessCategory(),
                    process.getBusinessType(),
                    process.getBusinessName(),
                    process.getBusinessAddress(),
                    process.getOpenDate() != null ? process.getOpenDate() : null
            );
            businessProfileRepository.save(businessProfile);

            // PERSONAL_INFO 약관 동의 이력 저장
            List<ConsentHistory> consentHistories = request.getConsents().stream()
                    .map(item -> ConsentHistory.builder()
                            .user(newUser)
                            .term(termMap.get(item.getTermId()))
                            .application(null)
                            .isConsented(item.getIsConsented())
                            .build())
                    .toList();
            consentHistoryRepository.saveAll(consentHistories);

            // S등급 산출 요청 레코드 생성
            SGradeHistory sGradeHistory = SGradeHistory.createRequested(newUser);
            sGradeHistoryRepository.save(sGradeHistory);

            // RegistrationProcess 삭제 (가입 완료)
            registrationProcessRepository.delete(process);

            return new SignupResult(newUser, sGradeHistory.getSGradeId());
        });

        // 비동기로 S등급 산출 요청 (회원가입 응답에 영향 없음)
        if (signupResult.sGradeId() != null) {
            sGradeService.predictAsync(signupResult.user().getUserId(), signupResult.sGradeId());
        }

        // 세션에서 registrationProcessId 제거
        session.removeAttribute(REGISTRATIONPROCESSID);
        log.info("회원가입 완료 userId={}", signupResult.user().getUserId());

        return AuthConverter.toSignupCompleteResponse(signupResult.user());
    }

    @Override
    @Transactional(readOnly = true)
    public CheckLoginIdResponse checkLoginId(String loginId) {
        // loginId 유효성 검증: 영문/숫자 4~20자
        if (loginId == null || !loginId.matches("^[a-zA-Z0-9]{4,20}$")) {
            throw new BaseException(AuthErrorCode.INVALID_LOGIN_ID_FORMAT);
        }

        boolean exists = userRepository.existsByLoginId(loginId);
        return new CheckLoginIdResponse(loginId, !exists);
    }

    @Override
    @AuditLog(action = "LOGIN", target = "사용자 로그인")
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String loginId = request.getLoginId();
        String ipAddress = getClientIp(httpRequest);

        // 0. 브루트포스 방어: IP 또는 계정 잠금 시 차단
        if (loginAttemptService.isBlocked(loginId, ipAddress)) {
            throw new BaseException(AuthErrorCode.ACCOUNT_LOCKED);
        }

        // 1. loginId로 사용자 조회 (미존재 시 동일 에러)
        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> {
                    log.warn("사용자 로그인 실패 loginId={}", LogMaskUtil.maskLoginId(loginId));
                    loginAttemptService.loginFailed(loginId, ipAddress);
                    return new BaseException(AuthErrorCode.LOGIN_FAILED);
                });

        // 2. 탈퇴 계정 체크
        if (user.getStatus() == UserStatus.INACTIVE) {
            log.warn("탈퇴 계정 로그인 시도 userId={}", user.getUserId());
            loginAttemptService.loginFailed(loginId, ipAddress);
            throw new BaseException(AuthErrorCode.ACCOUNT_WITHDRAWN);
        }

        // 3. 비밀번호 검증 (불일치 시 동일 에러 — Timing Attack 방지)
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("사용자 로그인 실패 loginId={}", LogMaskUtil.maskLoginId(loginId));
            loginAttemptService.loginFailed(loginId, ipAddress);
            throw new BaseException(AuthErrorCode.LOGIN_FAILED);
        }

        // 로그인 성공: 실패 카운트 초기화
        loginAttemptService.loginSucceeded(loginId);

        // 4. SecurityContext에 인증 정보 저장
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getUserId(), null,
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))
                );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 5. 세션에 principal 인덱스 설정 (동시 세션 조회에 필요)
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute(
                FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME,
                user.getUserId().toString()
        );

        // 6. 동시 세션 제어: 기존 세션 만료 + 새 세션 등록
        sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

        // 7. HttpSessionSecurityContextRepository를 통해 세션에 영속화
        securityContextRepository.saveContext(securityContext, httpRequest, httpResponse);

        // 8. 세션에 절대 만료 체크용 loginTime 저장
        session.setAttribute("loginTime", LocalDateTime.now());
        log.info("사용자 로그인 userId={}", user.getUserId());

        return AuthConverter.toLoginResponse(user);
    }

    @Override
    @AuditLog(action = "LOGOUT", target = "사용자 로그아웃")
    public void logout(HttpServletRequest request) {
        // 1. 세션 무효화 (Redis에서 삭제) — 먼저 수행하여 해당 세션으로의 추가 요청 차단
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // 2. SecurityContext 클리어 — 현재 스레드의 인증 정보 제거
        SecurityContextHolder.clearContext();
        log.info("사용자 로그아웃");
    }

    /**
     * 클라이언트 IP를 추출한다.
     * 프록시/로드밸런서 뒤에 있을 경우 X-Forwarded-For 헤더를 우선 사용한다.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // 여러 프록시를 거친 경우 첫 번째가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
