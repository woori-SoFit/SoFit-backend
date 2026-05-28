package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.converter.MyBizDataDetailConverter;
import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.loan.enums.ApplicationStatus;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.repository.LoanApplicationRepository;
import com.sofit.common.repository.MyBizDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyBizDataDetailServiceImpl implements MyBizDataDetailService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final MyBizDataRepository myBizDataRepository;

    @Override
    public MyBizDataDetailResponse findMyBizDataDetail(Long applicationId) {
        // 1. LoanApplication 조회
        LoanApplication app = loanApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 2. LoanApplication의 biz_data_id로 MyBizData 조회
        Long bizDataId = app.getBizDataId();
        if (bizDataId == null) {
            throw new BaseException(GeneralErrorCode.NOT_FOUND);
        }
        MyBizData myBizData = myBizDataRepository.findById(bizDataId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 3. 보유 대출 건수 산출 (status = EXECUTED)
        Long userId = app.getUser().getUserId();
        int existingLoanCount = loanApplicationRepository
                .countByUser_UserIdAndStatus(userId, ApplicationStatus.EXECUTED);

        // 4. Converter로 DTO 변환
        return MyBizDataDetailConverter.toMyBizDataDetailResponse(myBizData, existingLoanCount);
    }
}
