package com.sofit.user.domain.mybiz.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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

        // 3. 직전 월 데이터 조회 (rankChange 계산용)
        Optional<MyBizData> prevMonthData = myBizDataRepository
                .findByBusinessNumberAndReferenceMonth(businessNumber, referenceMonth.minusMonths(1));

        // 4. rankChange 계산 (직전 월 데이터 부재 시 null)
        BigDecimal salesRankChange = calculateRankChange(
                baseData.getIndustrySalesRank(),
                prevMonthData.map(MyBizData::getIndustrySalesRank).orElse(null));
        BigDecimal profitRankChange = calculateRankChange(
                baseData.getIndustryProfitRank(),
                prevMonthData.map(MyBizData::getIndustryProfitRank).orElse(null));
        BigDecimal stabilityRankChange = calculateRankChange(
                baseData.getIndustryStabilityRank(),
                prevMonthData.map(MyBizData::getIndustryStabilityRank).orElse(null));

        // 5. 5개월 추이 조회 (revenueTrend / ratingTrend 공통, 기준월 포함 이전 5개월, 오름차순)
        List<MyBizData> fiveMonthTrendData = myBizDataRepository
                .findByBusinessNumberAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                        businessNumber, referenceMonth.minusMonths(4), referenceMonth);

        // 6. cashFlowTrend 조회 (기준월 포함 이전 3개월, 오름차순)
        List<MyBizData> cashFlowTrendData = myBizDataRepository
                .findByBusinessNumberAndReferenceMonthBetweenOrderByReferenceMonthAsc(
                        businessNumber, referenceMonth.minusMonths(2), referenceMonth);

        // 7. 드롭다운용 전체 월 목록 조회 (referenceMonth만 내림차순)
        List<LocalDate> availableMonths = myBizDataRepository
                .findReferenceMonthsByBusinessNumber(businessNumber);

        // 8. Converter로 DTO 변환 후 반환
        return MyBizConverter.toMyBizDashboardResponse(
                baseData, fiveMonthTrendData, cashFlowTrendData, availableMonths,
                salesRankChange, profitRankChange, stabilityRankChange);
    }

    private String getBusinessNumber(Long userId) {
        BusinessProfile profile = businessProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BaseException(BusinessErrorCode.BUSINESS_PROFILE_NOT_FOUND));
        return profile.getBusinessNumber();
    }

    private BigDecimal calculateRankChange(BigDecimal currentRank, BigDecimal prevRank) {
        if (currentRank == null || prevRank == null) {
            return null;
        }
        return currentRank.subtract(prevRank);
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
        LocalDate referenceMonth = LocalDate.of(year, monthValue, 1);

        // 특정 월 데이터 조회
        return myBizDataRepository.findByBusinessNumberAndReferenceMonth(businessNumber, referenceMonth)
                .orElseThrow(() -> new BaseException(MyBizErrorCode.MY_BIZ_DATA_NOT_FOUND));
    }
}
