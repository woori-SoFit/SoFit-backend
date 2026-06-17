package com.sofit.common.entity.mybiz;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.sofit.common.entity.BaseEntity;
import com.sofit.common.entity.mybiz.enums.InsurancePaymentStatus;
import com.sofit.common.entity.mybiz.enums.VatFilingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "my_biz_data", indexes = {
        @Index(name = "idx_mybizdata_biznum_refmonth",
                columnList = "business_number, reference_month")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MyBizData extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "biz_data_id")
    private Long bizDataId;

    @Column(name = "business_number", nullable = false, length = 20)
    private String businessNumber;

    @Column(name = "industry_code", length = 20)
    private String industryCode;

    @Column(name = "industry_name", length = 100)
    private String industryName;

    @Column(name = "district_code", length = 20)
    private String districtCode;

    @Column(name = "reference_month", nullable = false)
    private LocalDate referenceMonth;

    @Column(name = "business_age_months")
    private Integer businessAgeMonths;

    // --- 매출 ---

    @Column(name = "monthly_revenue")
    private Long monthlyRevenue;

    @Column(name = "monthly_outflow")
    private Long monthlyOutflow;

    @Column(name = "estimated_profit")
    private Long estimatedProfit;

    @Column(name = "prev_month_revenue")
    private Long prevMonthRevenue;

    @Column(name = "monthly_profit_rate", precision = 5, scale = 2)
    private BigDecimal monthlyProfitRate;

    @Column(name = "monthly_profit_growth_rate", precision = 5, scale = 2)
    private BigDecimal monthlyProfitGrowthRate;

    @Column(name = "annual_income")
    private Long annualIncome;

    @Column(name = "pos_sales_amount")
    private Long posSalesAmount;

    @Column(name = "avg_revenue_mon")
    private Long avgRevenueMon;

    @Column(name = "avg_revenue_tue")
    private Long avgRevenueTue;

    @Column(name = "avg_revenue_wed")
    private Long avgRevenueWed;

    @Column(name = "avg_revenue_thu")
    private Long avgRevenueThu;

    @Column(name = "avg_revenue_fri")
    private Long avgRevenueFri;

    @Column(name = "avg_revenue_sat")
    private Long avgRevenueSat;

    @Column(name = "avg_revenue_sun")
    private Long avgRevenueSun;

    // --- 거래 ---

    @Column(name = "monthly_payment_amount", precision = 15, scale = 2)
    private BigDecimal monthlyPaymentAmount;

    @Column(name = "monthly_payment_count")
    private Integer monthlyPaymentCount;

    @Column(name = "avg_payment_amount", precision = 15, scale = 2)
    private BigDecimal avgPaymentAmount;

    @Column(name = "days_since_last_transaction")
    private Integer daysSinceLastTransaction;

    @Column(name = "max_inactive_days")
    private Integer maxInactiveDays;

    // --- 배달/온라인 ---

    @Column(name = "delivery_sales_amount")
    private Long deliverySalesAmount;

    @Column(name = "delivery_order_count")
    private Integer deliveryOrderCount;

    @Column(name = "delivery_rating", precision = 3, scale = 1)
    private BigDecimal deliveryRating;

    @Column(name = "online_reorder_rate", precision = 5, scale = 2)
    private BigDecimal onlineReorderRate;

    @Column(name = "online_reply_rate", precision = 5, scale = 2)
    private BigDecimal onlineReplyRate;

    @Column(name = "online_info_update_count")
    private Integer onlineInfoUpdateCount;

    // --- 리뷰/평점 ---

    @Column(name = "review_rating", precision = 3, scale = 1)
    private BigDecimal reviewRating;

    @Column(name = "review_count")
    private Integer reviewCount;

    @Column(name = "positive_review_ratio", precision = 5, scale = 2)
    private BigDecimal positiveReviewRatio;

    @Column(name = "negative_review_ratio", precision = 5, scale = 2)
    private BigDecimal negativeReviewRatio;

    // --- 업종 순위 ---

    @Column(name = "industry_sales_rank", precision = 5, scale = 2)
    private BigDecimal industrySalesRank;

    @Column(name = "industry_profit_rank", precision = 5, scale = 2)
    private BigDecimal industryProfitRank;

    @Column(name = "industry_satisfaction_rank", precision = 5, scale = 2)
    private BigDecimal industrySatisfactionRank;

    @Column(name = "district_sales_rank", precision = 5, scale = 2)
    private BigDecimal districtSalesRank;

    @Column(name = "district_profit_rank", precision = 5, scale = 2)
    private BigDecimal districtProfitRank;

    @Column(name = "district_satisfaction_rank", precision = 5, scale = 2)
    private BigDecimal districtSatisfactionRank;

    // --- 업종/상권 평균 ---

    @Column(name = "industry_avg_revenue")
    private Long industryAvgRevenue;

    @Column(name = "industry_avg_profit_rate", precision = 5, scale = 2)
    private BigDecimal industryAvgProfitRate;

    @Column(name = "industry_avg_review_rating", precision = 3, scale = 1)
    private BigDecimal industryAvgReviewRating;

    @Column(name = "district_avg_revenue")
    private Long districtAvgRevenue;

    @Column(name = "district_avg_profit_rate", precision = 5, scale = 2)
    private BigDecimal districtAvgProfitRate;

    @Column(name = "district_avg_review_rating", precision = 3, scale = 1)
    private BigDecimal districtAvgReviewRating;

    // --- 세금/보험 ---

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_filing_status", length = 10)
    private VatFilingStatus vatFilingStatus;

    @Column(name = "vat_filing_date")
    private LocalDate vatFilingDate;

    @Column(name = "tax_overdue")
    private Boolean taxOverdue;

    @Enumerated(EnumType.STRING)
    @Column(name = "insurance_payment_status", length = 10)
    private InsurancePaymentStatus insurancePaymentStatus;

    // --- 대출/DSR ---

    @Column(name = "existing_loan_count")
    private Integer existingLoanCount;

    @Column(name = "annual_repayment")
    private Long annualRepayment;

    @Column(name = "monthly_repayment")
    private Long monthlyRepayment;

    @Column(name = "total_loan_balance")
    private Long totalLoanBalance;

    // --- 인력/사업장 ---

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "has_sns")
    private Boolean hasSns;

    @Column(name = "has_online_reservation")
    private Boolean hasOnlineReservation;

    @Column(name = "is_near_subway")
    private Boolean isNearSubway;
}
