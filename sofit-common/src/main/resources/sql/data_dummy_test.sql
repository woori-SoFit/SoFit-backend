-- =====================================================
-- SoFit Dummy Data (테스트용 - USER 3명 축소 버전)
-- 실행 순서: 2번째 (data_static.sql 이후)
-- users → business_profile → my_biz_data → s_grade_feature
-- =====================================================

-- AUTO_INCREMENT 100부터 시작 (신규 가입 테스트 충돌 방지)
ALTER TABLE users AUTO_INCREMENT = 100;

-- =====================================================
-- 1. users (USER 3명 + 은행원 3명 + 지점장 1명 + 개발자 1명)
-- =====================================================
INSERT INTO users (
    user_id, login_id, name, password_hash, phone_number, resident_number,
    role, status, created_at, updated_at, inactivated_at
) VALUES
(1, 'user001', '강민수', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010001111', '9002141', 'USER', 'ACTIVE', '2024-01-15 00:00:00', '2024-01-15 00:00:00', NULL),
(2, 'user002', '이서아', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010002222', '9506112', 'USER', 'ACTIVE', '2024-02-10 00:00:00', '2024-02-10 00:00:00', NULL),
(3, 'user003', '박지훈', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010003333', '8809031', 'USER', 'ACTIVE', '2024-03-20 00:00:00', '2024-03-20 00:00:00', NULL);

-- 은행원 3명, 지점장 1명, 개발자 1명
INSERT INTO users (
    user_id, login_id, password_hash, name,
    phone_number, resident_number, role, status, created_at, updated_at)
VALUES
    (11, 'teller_01', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '김은행', '01011111111', '9001011', 'ADMIN_BANK_TELLER', 'ACTIVE', NOW(), NOW()),
    (12, 'teller_02', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '이창구', '01011111112', '9101011', 'ADMIN_BANK_TELLER', 'ACTIVE', NOW(), NOW()),
    (13, 'teller_03', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '최심사', '01011111113', '9201011', 'ADMIN_BANK_TELLER', 'ACTIVE', NOW(), NOW()),
    (14, 'manager_01', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '박지점장', '01022222222', '8501012', 'ADMIN_BANK_MANAGER', 'ACTIVE', NOW(), NOW()),
    (15, 'dev_01',  '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '이개발',   '01033333333', '9201013', 'ADMIN_DEV',          'ACTIVE', NOW(), NOW());

-- =====================================================
-- 2. business_profile (USER 3명분만)
-- =====================================================
INSERT INTO business_profile (
    user_id, business_number, business_name,
    representative_name, business_category, business_type,
    business_address, open_date,
    is_mybiz_connected, mybiz_connected_at,
    created_at, updated_at
) VALUES
(1, '1012345678', '민수분식',   '강민수', '음식점업',        '분식',   '서울시 관악구 신림로 120',    '2019-02-18', false, NULL, '2024-01-15 00:00:00', '2024-01-15 00:00:00'),
(2, '1023456789', '서아카페',   '이서아', '비알코올음료점업', '카페·커피전문점', '서울시 동작구 상도로 88', '2020-06-11', false, NULL, '2024-02-10 00:00:00', '2024-02-10 00:00:00'),
(3, '1034567890', '지훈식당',   '박지훈', '음식점업',        '한식',   '서울시 중구 명동길 45',       '2018-10-03', false, NULL, '2024-03-20 00:00:00', '2024-03-20 00:00:00');

-- =====================================================
-- 3. my_biz_data (USER 3명 사업자번호만)
-- =====================================================
INSERT INTO my_biz_data (business_number, industry_code, industry_name, district_code, reference_month, business_age_months, annual_income, monthly_revenue, monthly_outflow, estimated_profit, prev_month_revenue, monthly_profit_rate, monthly_profit_growth_rate, pos_sales_amount, delivery_sales_amount, avg_revenue_mon, avg_revenue_tue, avg_revenue_wed, avg_revenue_thu, avg_revenue_fri, avg_revenue_sat, avg_revenue_sun, monthly_payment_amount, monthly_payment_count, avg_payment_amount, days_since_last_transaction, max_inactive_days, delivery_order_count, delivery_rating, online_reorder_rate, online_reply_rate, online_info_update_count, review_rating, review_count, positive_review_ratio, negative_review_ratio, industry_sales_rank, industry_profit_rank, industry_satisfaction_rank, district_sales_rank, district_profit_rank, district_satisfaction_rank, industry_avg_revenue, industry_avg_profit_rate, industry_avg_review_rating, district_avg_revenue, district_avg_profit_rate, district_avg_review_rating, vat_filing_status, vat_filing_date, tax_overdue, insurance_payment_status, employee_count, has_sns, has_online_reservation, is_near_subway, created_at, updated_at) VALUES
-- 강민수 (1012345678) 최신월
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-05-01', 36, 240000000, 21000000, 18500000, 5200000, 19500000, 24.76, 6.12, 13200000, 7800000, 580000, 550000, 620000, 660000, 820000, 950000, 720000, 21000000.00, 820, 25609.76, 1, 3, 340, 4.6, 38.50, 92.00, 6, 4.7, 510, 88.20, 6.50, 18.50, 21.30, 20.10, 15.20, 18.40, 19.80, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-05-10', false, 'PAID', 5, true, true, true, NOW(), NOW()),
-- 이서아 (1023456789)
('1023456789', 'I56192', '비알코올음료점업', 'SEOUL-MP', '2026-05-01', 18, 132000000, 11500000, 9900000, 3100000, 10800000, 26.96, 6.90, 7300000, 4200000, 320000, 310000, 350000, 370000, 460000, 530000, 380000, 11500000.00, 640, 17968.75, 2, 5, 210, 4.5, 42.00, 89.00, 8, 4.6, 380, 85.40, 8.20, 32.00, 35.50, 33.10, 28.50, 30.20, 32.80, 9800000, 24.00, 4.4, 10100000, 23.50, 4.3, 'FILED', '2026-05-11', false, 'PAID', 3, true, true, true, NOW(), NOW()),
-- 박지훈 (1034567890)
('1034567890', 'I56111', '음식점업(한식)', 'SEOUL-SD', '2026-05-01', 24, 180000000, 15800000, 13200000, 4300000, 14900000, 27.22, 5.10, 15800000, 0, 430000, 410000, 490000, 520000, 650000, 720000, 550000, 15800000.00, 530, 29811.32, 1, 4, 0, NULL, 55.00, 95.00, 12, 4.4, 240, 82.10, 10.20, 24.50, 29.00, 26.30, 21.80, 23.50, 27.10, 15800000, 22.50, 4.5, 14900000, 21.80, 4.4, 'FILED', '2026-05-09', false, 'PAID', 4, true, false, false, NOW(), NOW());

-- 강민수 (1012345678) 추가 5개월치 (이전 월 데이터)
INSERT INTO my_biz_data (business_number, industry_code, industry_name, district_code, reference_month, business_age_months, annual_income, monthly_revenue, monthly_outflow, estimated_profit, prev_month_revenue, monthly_profit_rate, monthly_profit_growth_rate, pos_sales_amount, delivery_sales_amount, avg_revenue_mon, avg_revenue_tue, avg_revenue_wed, avg_revenue_thu, avg_revenue_fri, avg_revenue_sat, avg_revenue_sun, monthly_payment_amount, monthly_payment_count, avg_payment_amount, days_since_last_transaction, max_inactive_days, delivery_order_count, delivery_rating, online_reorder_rate, online_reply_rate, online_info_update_count, review_rating, review_count, positive_review_ratio, negative_review_ratio, industry_sales_rank, industry_profit_rank, industry_satisfaction_rank, district_sales_rank, district_profit_rank, district_satisfaction_rank, industry_avg_revenue, industry_avg_profit_rate, industry_avg_review_rating, district_avg_revenue, district_avg_profit_rate, district_avg_review_rating, vat_filing_status, vat_filing_date, tax_overdue, insurance_payment_status, employee_count, has_sns, has_online_reservation, is_near_subway, created_at, updated_at) VALUES
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2025-12-01', 31, 240000000, 16800000, 15100000, 4100000, 16200000, 24.40, 4.60, 10700000, 6100000, 460000, 440000, 500000, 530000, 660000, 760000, 580000, 16800000.00, 710, 23661.97, 1, 3, 270, 4.5, 35.00, 88.00, 4, 4.6, 440, 86.10, 7.20, 22.50, 25.00, 23.40, 18.90, 21.10, 24.30, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2025-12-10', false, 'PAID', 5, true, true, true, NOW(), NOW()),
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-01-01', 32, 240000000, 17400000, 15600000, 4300000, 16800000, 24.71, 4.88, 11000000, 6400000, 480000, 455000, 515000, 550000, 680000, 790000, 600000, 17400000.00, 730, 23835.62, 1, 3, 285, 4.5, 36.00, 89.00, 5, 4.6, 460, 86.50, 7.10, 21.50, 24.00, 22.80, 18.30, 20.50, 23.70, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-01-10', false, 'PAID', 5, true, true, true, NOW(), NOW()),
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-02-01', 33, 240000000, 18100000, 16100000, 4500000, 17400000, 24.86, 4.65, 11400000, 6700000, 500000, 475000, 535000, 570000, 710000, 820000, 620000, 18100000.00, 755, 23973.51, 1, 3, 300, 4.6, 36.50, 90.00, 5, 4.7, 475, 87.00, 6.90, 21.00, 23.00, 22.10, 17.80, 19.90, 23.10, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-02-10', false, 'PAID', 5, true, true, true, NOW(), NOW()),
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-03-01', 34, 240000000, 19000000, 17000000, 4700000, 18100000, 24.74, 4.44, 11900000, 7100000, 520000, 495000, 560000, 595000, 740000, 860000, 650000, 19000000.00, 780, 24358.97, 1, 3, 315, 4.6, 37.00, 91.00, 6, 4.7, 490, 87.50, 6.80, 20.00, 22.00, 21.30, 17.10, 19.20, 22.40, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-03-10', false, 'PAID', 5, true, true, true, NOW(), NOW()),
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-04-01', 35, 240000000, 19500000, 17800000, 4900000, 19000000, 25.13, 4.26, 12100000, 7400000, 535000, 510000, 575000, 610000, 760000, 880000, 665000, 19500000.00, 800, 24375.00, 1, 3, 328, 4.6, 37.50, 91.00, 6, 4.7, 500, 87.80, 6.70, 19.50, 21.50, 20.80, 16.60, 18.70, 21.90, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-04-10', false, 'PAID', 5, true, true, true, NOW(), NOW());

-- =====================================================
-- 4. s_grade_feature (USER 3명분만, biz_data_id 1~3)
-- =====================================================
INSERT INTO s_grade_feature (
    biz_data_id,
    business_age_months, quarterly_revenue_growth_rate, annual_revenue_growth_rate,
    revenue_vs_industry_avg_ratio,
    avg_monthly_transaction_3m, avg_monthly_transaction_6m, avg_monthly_transaction_12m,
    days_since_last_transaction, max_inactive_days, online_platform_activity_index,
    revenue_growth_per_employee_3m, revenue_growth_per_employee_6m, revenue_growth_per_employee_12m,
    revenue_growth_per_business_age_3m, revenue_growth_per_business_age_6m, revenue_growth_per_business_age_12m,
    online_accessibility_score, is_near_subway,
    commercial_saturation_score, is_traditional_market, commercial_trend, industry_trend,
    review_rating, review_count, delivery_rating, delivery_order_count,
    positive_review_ratio, has_online_reservation,
    owner_experience_years, employee_count, has_sns,
    created_at
) VALUES
-- 강민수 | 음식점업(분식) | 업종평균 1200만 | ratio=1.75
(1, 36, 11.54, 28.85, 1.75, 21000000.0, 19950000.0, 18900000.0, 1, 3, 70.2, 2.31, 2.54, 5.77, 0.3206, 0.3526, 0.8014, 98.0, TRUE, 45.0, FALSE, 'GROWING', 'GROWING', 4.7, 510, 4.6, 340, 88.2, TRUE, 4, 5, TRUE, NOW()),
-- 이서아 | 비알코올음료점업 | 업종평균 800만 | ratio=1.44
(2, 18, 9.72, 24.30, 1.44, 11500000.0, 10925000.0, 10350000.0, 2, 5, 72.4, 3.24, 3.56, 8.10, 0.5400, 0.5940, 1.3500, 100.0, TRUE, 55.0, FALSE, 'GROWING', 'STABLE', 4.6, 380, 4.5, 210, 85.4, TRUE, 2, 3, TRUE, NOW()),
-- 박지훈 | 음식점업(한식) | 업종평균 1200만 | ratio=1.32
(3, 24, 9.06, 22.65, 1.32, 15800000.0, 15010000.0, 14220000.0, 1, 4, 80.0, 2.27, 2.49, 5.66, 0.3775, 0.4153, 0.9437, 75.0, FALSE, 60.0, FALSE, 'STABLE', 'GROWING', 4.4, 240, NULL, 0, 82.1, FALSE, 3, 4, TRUE, NOW());
