package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.LoanApplicationInfoConverter;
import com.sofit.admin.domain.loan.dto.response.LoanApplicationInfoResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.term.ConsentHistory;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.term.ConsentHistoryRepository;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LoanApplicationInfoServiceImpl implements LoanApplicationInfoService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final UserRepository userRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final ConsentHistoryRepository consentHistoryRepository;

    @Override
    public LoanApplicationInfoResponse findLoanApplicationInfo(Long applicationId) {
        // 1. LoanApplication 조회
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        Long userId = app.getUser().getUserId();

        // 2. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 3. BusinessProfile 조회 (대출 신청 시 필수이므로 반드시 존재)
        BusinessProfile businessProfile = businessProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 4. ConsentHistory 조회 (user_id + application_id 기준, consent_id 오름차순)
        List<ConsentHistory> consentHistories = consentHistoryRepository
                .findByUser_UserIdAndApplication_ApplicationIdOrderByConsentIdAsc(userId, applicationId);

        // 5. Converter로 DTO 변환
        return LoanApplicationInfoConverter.toLoanApplicationInfoResponse(
                user, businessProfile, app, consentHistories);
    }
}
