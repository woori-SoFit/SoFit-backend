package com.sofit.admin.domain.loan.converter;

import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;
import com.sofit.common.entity.mybiz.MyBizData;

public class MyBizDataDetailConverter {

    private MyBizDataDetailConverter() {}

    public static MyBizDataDetailResponse toMyBizDataDetailResponse(
            MyBizData myBizData, int existingLoanCount) {

        return new MyBizDataDetailResponse(
                myBizData.getAnnualIncome(),
                existingLoanCount,
                myBizData.getMonthlyRevenue(),
                myBizData.getMonthlyProfitGrowthRate(),
                myBizData.getBusinessAgeMonths(),
                myBizData.getVatFilingStatus() != null ? myBizData.getVatFilingStatus().name() : null,
                myBizData.getTaxOverdue(),
                myBizData.getInsurancePaymentStatus() != null ? myBizData.getInsurancePaymentStatus().name() : null,
                myBizData.getIndustrySalesRank(),
                myBizData.getIndustryProfitRank()
        );
    }
}
