package com.sofit.user.domain.loan.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.loan.enums.LastCompletedStep;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.mybiz.MyBizDataRepository;
import com.sofit.user.domain.loan.exception.LoanErrorCode;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.service.TermService;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import com.sofit.user.domain.user.service.BusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LoanStepServiceImpl implements LoanStepService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final MyBizDataRepository myBizDataRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final TermService termService;
    private final BusinessService businessService;

    // Step 2: 대출 약관 동의
    @Override
    public ConsentCreateResponse processConsent(Long userId, Long applicationId, ConsentCreateRequest request) {
        LoanApplication application = validateAndGetApplication(userId, applicationId, null);

        ConsentCreateResponse response = termService.createConsents(userId, request);

        application.updateLastCompletedStep(LastCompletedStep.CONSENT_DONE);
        return response;
    }

    // Step 3: 사업자 정보 확인
    @Override
    public BusinessProfileResponse processBizInfo(Long userId, Long applicationId) {
        LoanApplication application = validateAndGetApplication(userId, applicationId, LastCompletedStep.CONSENT_DONE);

        BusinessProfileResponse response = businessService.findBusinessProfile(userId);

        application.updateLastCompletedStep(LastCompletedStep.BIZ_INFO_DONE);
        return response;
    }

    // Step 4: 마이데이터 약관 동의
    @Override
    public ConsentCreateResponse processMydata(Long userId, Long applicationId, ConsentCreateRequest request) {
        LoanApplication application = validateAndGetApplication(userId, applicationId, LastCompletedStep.BIZ_INFO_DONE);

        ConsentCreateResponse response = termService.createConsents(userId, request);

        application.updateLastCompletedStep(LastCompletedStep.DATA_COLLECTED);
        return response;
    }

    // Step 5: 마이비즈데이터 연동 완료 
    @Override
    public void processMybizData(Long userId, Long applicationId) {
        LoanApplication application = validateAndGetApplication(userId, applicationId, LastCompletedStep.DATA_COLLECTED);

        // 1. userId → businessNumber 조회
        BusinessProfile profile = businessProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.MYBIZ_DATA_NOT_FOUND));
        String businessNumber = profile.getBusinessNumber();

        // 2. businessNumber로 최신 biz_data_id 조회
        MyBizData latestBizData = myBizDataRepository.findFirstByBusinessNumberOrderByReferenceMonthDesc(businessNumber)
                .orElseThrow(() -> new BaseException(LoanErrorCode.MYBIZ_DATA_NOT_FOUND));

        // 3. loan_application.biz_data_id 업데이트
        application.updateBizDataId(latestBizData.getBizDataId());

        // 4. business_profile 연동 여부 업데이트
        businessService.connectMybiz(userId);

        // 5. lastCompletedStep = MYBIZ_CONNECTED
        application.updateLastCompletedStep(LastCompletedStep.MYBIZ_CONNECTED);
    }

    // 공통 검증

    /**
     * 공통 검증 로직.
     * 검증 순서: 존재 확인(404) → 본인 소유(403) → DRAFT 상태(400) → 단계 순서(400)
     */
    private LoanApplication validateAndGetApplication(Long userId, Long applicationId,
                                                      LastCompletedStep requiredStep) {
        // 1. 존재 확인
        LoanApplication application = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(LoanErrorCode.APPLICATION_NOT_FOUND));

        // 2. 본인 소유 확인
        if (!Objects.equals(application.getUser().getUserId(), userId)) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_OWNED);
        }

        // 3. DRAFT 상태 확인
        if (application.getStatus() != ApplicationStatus.DRAFT) {
            throw new BaseException(LoanErrorCode.APPLICATION_NOT_DRAFT);
        }

        // 4. 단계 순서 확인
        if (!Objects.equals(application.getLastCompletedStep(), requiredStep)) {
            throw new BaseException(LoanErrorCode.STEP_ORDER_VIOLATION);
        }

        return application;
    }
}
