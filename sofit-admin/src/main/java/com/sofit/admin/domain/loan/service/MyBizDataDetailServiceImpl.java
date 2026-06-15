package com.sofit.admin.domain.loan.service;

import java.util.List;

import com.sofit.admin.domain.loan.converter.MyBizDataDetailConverter;
import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.loan.LoanApplication;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.repository.loan.LoanApplicationRepository;
import com.sofit.common.repository.mybiz.MyBizDataRepository;
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

        // 2. LoanApplication의 biz_data_id로 MyBizData 조회 (기준 월)
        Long bizDataId = app.getBizDataId();
        if (bizDataId == null) {
            throw new BaseException(GeneralErrorCode.NOT_FOUND);
        }
        MyBizData baseData = myBizDataRepository.findById(bizDataId)
                .orElseThrow(() -> new BaseException(GeneralErrorCode.NOT_FOUND));

        // 3. 6개월 추이 데이터 조회 (기준월 포함 이전 6개월, 오름차순)
        List<MyBizData> sixMonthTrendData = myBizDataRepository
                .findByBusinessNumberAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                        baseData.getBusinessNumber(),
                        baseData.getReferenceMonth().minusMonths(5),
                        baseData.getReferenceMonth());

        // 4. Converter로 DTO 변환
        return MyBizDataDetailConverter.toMyBizDataDetailResponse(baseData, sixMonthTrendData);
    }
}
