package com.sofit.user.domain.mybiz.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.apiPayload.code.GeneralErrorCode;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.mybiz.MyBizData;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.mybiz.MyBizDataRepository;
import com.sofit.user.domain.mybiz.converter.MyBizConverter;
import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;
import com.sofit.user.domain.mybiz.exception.MyBizErrorCode;
import com.sofit.user.domain.user.exception.BusinessErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyBizServiceImpl implements MyBizService {

    private final MyBizDataRepository myBizDataRepository;
    private final BusinessProfileRepository businessProfileRepository;

    private static final Pattern MONTH_PATTERN = Pattern.compile("^\\d{4}-(0[1-9]|1[0-2])$");

    @Override
    public MyBizDashboardResponse findDashboard(Long userId, String month) {
        // 0. userId → businessNumber 조회
        String businessNumber = getBusinessNumber(userId);

        // 1. month 파라미터 검증 및 기준월 데이터 조회
        MyBizData baseData = resolveBaseData(businessNumber, month);

        // 2. 기준월의 referenceMonth 추출
        LocalDate referenceMonth = baseData.getReferenceMonth();

        // 3. 6개월 추이 조회 (revenueTrend + paymentFlowTrend 공용, 기준월 포함 이전 6개월, 오름차순)
        List<MyBizData> sixMonthTrendData = myBizDataRepository
                .findByBusinessNumberAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                        businessNumber, referenceMonth.minusMonths(5), referenceMonth);

        // 4. monthlyRevenueGrowthRate 계산 (prevMonthRevenue가 null이거나 0이면 null)
        BigDecimal monthlyRevenueGrowthRate = MyBizConverter.calculateMonthlyRevenueGrowthRate(
                baseData.getMonthlyRevenue(), baseData.getPrevMonthRevenue());

        // 5. 드롭다운용 전체 월 목록 조회 (referenceMonth만 내림차순)
        List<LocalDate> availableMonths = myBizDataRepository
                .findReferenceMonthsByBusinessNumber(businessNumber);

        // 6. Converter로 DTO 변환 후 반환
        return MyBizConverter.toMyBizDashboardResponse(
                baseData, sixMonthTrendData, availableMonths, monthlyRevenueGrowthRate);
    }

    private String getBusinessNumber(Long userId) {
        BusinessProfile profile = businessProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BaseException(BusinessErrorCode.BUSINESS_PROFILE_NOT_FOUND));
        return profile.getBusinessNumber();
    }

    private MyBizData resolveBaseData(String businessNumber, String month) {
        if (month == null || month.isBlank()) {
            // null 또는 빈 문자열 → 최신 데이터 조회
            return myBizDataRepository.findFirstByBusinessNumberOrderByReferenceMonthDesc(businessNumber)
                    .orElseThrow(() -> new BaseException(MyBizErrorCode.MY_BIZ_DATA_NOT_FOUND));
        }

        if (!MONTH_PATTERN.matcher(month).matches()) {
            // yyyy-MM 정규식에 매칭되지 않음 → BAD_REQUEST
            throw new BaseException(GeneralErrorCode.BAD_REQUEST);
        }

        // yyyy-MM 형식 파싱 → LocalDate (해당 월 1일)
        String[] parts = month.split("-");
        int year = Integer.parseInt(parts[0]);
        int monthValue = Integer.parseInt(parts[1]);
        LocalDate referenceMonthDate = LocalDate.of(year, monthValue, 1);

        // 특정 월 데이터 조회
        return myBizDataRepository.findByBusinessNumberAndReferenceMonth(businessNumber, referenceMonthDate)
                .orElseThrow(() -> new BaseException(MyBizErrorCode.MY_BIZ_DATA_NOT_FOUND));
    }
}
