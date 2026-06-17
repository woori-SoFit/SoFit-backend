-- =====================================================
-- SoFit Dummy Data (테스트용 더미 데이터 - user 포함)
-- 실행 순서: 2번째 (data_static.sql 이후)
-- users → business_profile → my_biz_data → s_grade_feature
-- =====================================================

-- AUTO_INCREMENT 11부터 시작 (신규 가입 테스트 충돌 방지)
ALTER TABLE users AUTO_INCREMENT = 100;

-- =====================================================
-- 1. users
-- =====================================================
INSERT INTO users (
    user_id, login_id, name, password_hash, phone_number, resident_number,
    role, status, created_at, updated_at, inactivated_at
) VALUES
(1,  'user001', '강민수', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010001111', '9002141', 'USER', 'ACTIVE', '2024-01-15 00:00:00', '2024-01-15 00:00:00', NULL),
(2,  'user002', '이서아', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010002222', '9506112', 'USER', 'ACTIVE', '2024-02-10 00:00:00', '2024-02-10 00:00:00', NULL),
(3,  'user003', '박지훈', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010003333', '8809031', 'USER', 'ACTIVE', '2024-03-20 00:00:00', '2024-03-20 00:00:00', NULL),
(4,  'user004', '최유진', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010004444', '9301252', 'USER', 'ACTIVE', '2024-04-05 00:00:00', '2024-04-05 00:00:00', NULL),
(5,  'user005', '정우진', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010005555', '8708141', 'USER', 'ACTIVE', '2024-05-01 00:00:00', '2024-05-01 00:00:00', NULL),
(6,  'user006', '김나은', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010006666', '9604092', 'USER', 'ACTIVE', '2024-06-12 00:00:00', '2024-06-12 00:00:00', NULL),
(7,  'user007', '윤태현', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010007777', '8512011', 'USER', 'ACTIVE', '2024-07-03 00:00:00', '2024-07-03 00:00:00', NULL),
(8,  'user008', '송하린', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010008888', '9703172', 'USER', 'ACTIVE', '2024-08-22 00:00:00', '2024-08-22 00:00:00', NULL),
(9,  'user009', '장도현', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010009999', '8407221', 'USER', 'ACTIVE', '2024-09-09 00:00:00', '2024-09-09 00:00:00', NULL),
(10, 'user010', '임수빈', '$2a$10$Wk3FUAb81n.4/Vjgw2aRqexj/9pXPKnL/nJoFmsvxaA6S0/HlDTc6', '01010100000', '9205302', 'USER', 'ACTIVE', '2024-10-18 00:00:00', '2024-10-18 00:00:00', NULL);

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
-- 2. business_profile
-- =====================================================
INSERT INTO business_profile (
    user_id, business_number, business_name,
    representative_name, business_category, business_type,
    business_address, open_date,
    is_mybiz_connected, mybiz_connected_at,
    created_at, updated_at
) VALUES
(1,  '1012345678', '민수분식',        '강민수', '음식점업',        '분식',           '서울시 관악구 신림로 120',         '2019-02-18', false, NULL, '2024-01-15 00:00:00', '2024-01-15 00:00:00'),
(2,  '1023456789', '서아카페',        '이서아', '비알코올음료점업', '카페·커피전문점', '서울시 동작구 상도로 88',          '2020-06-11', false, NULL, '2024-02-10 00:00:00', '2024-02-10 00:00:00'),
(3,  '1034567890', '지훈식당',        '박지훈', '음식점업',        '한식',           '서울시 중구 명동길 45',            '2018-10-03', false, NULL, '2024-03-20 00:00:00', '2024-03-20 00:00:00'),
(4,  '1045678901', '유진베이커리',    '최유진', '비알코올음료점업', '제과·베이커리',  '서울시 성동구 왕십리로 210',        '2021-01-25', false, NULL, '2024-04-05 00:00:00', '2024-04-05 00:00:00'),
(5,  '1056789012', '우진게스트하우스', '정우진', '숙박업',          '게스트하우스',   '서울시 마포구 월드컵북로 77',       '2017-08-14', false, NULL, '2024-05-01 00:00:00', '2024-05-01 00:00:00'),
(6,  '1067890123', '나은카페',        '김나은', '비알코올음료점업', '카페·커피전문점', '경기도 고양시 일산동구 중앙로 100', '2016-04-09', false, NULL, '2024-06-12 00:00:00', '2024-06-12 00:00:00'),
(7,  '1078901234', '태현치킨',        '윤태현', '음식점업',        '치킨',           '경기도 안양시 동안구 시민대로 180', '2015-12-01', false, NULL, '2024-07-03 00:00:00', '2024-07-03 00:00:00'),
(8,  '1089012345', '하린디저트카페',  '송하린', '비알코올음료점업', '카페·커피전문점', '서울시 강남구 논현로 250',         '2022-03-17', false, NULL, '2024-08-22 00:00:00', '2024-08-22 00:00:00'),
(9,  '1090123456', '도현고깃집',      '장도현', '음식점업',        '한식',           '인천시 서구 가정로 55',            '2014-07-22', false, NULL, '2024-09-09 00:00:00', '2024-09-09 00:00:00'),
(10, '1101234567', '수빈식당',        '임수빈', '음식점업',        '한식',           '대구시 중구 동성로 33',            '2018-05-30', false, NULL, '2024-10-18 00:00:00', '2024-10-18 00:00:00');

-- =====================================================
-- 3. my_biz_data
-- =====================================================
INSERT INTO my_biz_data (business_number, industry_code, industry_name, district_code, reference_month, business_age_months, annual_income, monthly_revenue, monthly_outflow, estimated_profit, prev_month_revenue, monthly_profit_rate, monthly_profit_growth_rate, pos_sales_amount, delivery_sales_amount, avg_revenue_mon, avg_revenue_tue, avg_revenue_wed, avg_revenue_thu, avg_revenue_fri, avg_revenue_sat, avg_revenue_sun, monthly_payment_amount, monthly_payment_count, avg_payment_amount, days_since_last_transaction, max_inactive_days, delivery_order_count, delivery_rating, online_reorder_rate, online_reply_rate, online_info_update_count, review_rating, review_count, positive_review_ratio, negative_review_ratio, industry_sales_rank, industry_profit_rank, industry_satisfaction_rank, district_sales_rank, district_profit_rank, district_satisfaction_rank, industry_avg_revenue, industry_avg_profit_rate, industry_avg_review_rating, district_avg_revenue, district_avg_profit_rate, district_avg_review_rating, vat_filing_status, vat_filing_date, tax_overdue, insurance_payment_status, existing_loan_count, annual_repayment, monthly_repayment, total_loan_balance, employee_count, has_sns, has_online_reservation, is_near_subway, created_at, updated_at) VALUES
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-05-01', 36, 240000000, 21000000, 18500000, 5200000, 19500000, 24.76, 6.12, 13200000, 7800000, 580000, 550000, 620000, 660000, 820000, 950000, 720000, 21000000.00, 820, 25609.76, 1, 3, 340, 4.6, 38.50, 92.00, 6, 4.7, 510, 88.20, 6.50, 18.50, 21.30, 20.10, 15.20, 18.40, 19.80, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-05-10', false, 'PAID', 2, 36000000, 3000000, 28000000, 5, true, true, true, NOW(), NOW()),
('1023456789', 'I56192', '비알코올음료점업', 'SEOUL-MP', '2026-05-01', 18, 132000000, 11500000, 9900000, 3100000, 10800000, 26.96, 6.90, 7300000, 4200000, 320000, 310000, 350000, 370000, 460000, 530000, 380000, 11500000.00, 640, 17968.75, 2, 5, 210, 4.5, 42.00, 89.00, 8, 4.6, 380, 85.40, 8.20, 32.00, 35.50, 33.10, 28.50, 30.20, 32.80, 9800000, 24.00, 4.4, 10100000, 23.50, 4.3, 'FILED', '2026-05-11', false, 'PAID', 1, 30400000, 2530000, 15000000, 3, true, true, true, NOW(), NOW()),
('1034567890', 'I56111', '음식점업(한식)', 'SEOUL-SD', '2026-05-01', 24, 180000000, 15800000, 13200000, 4300000, 14900000, 27.22, 5.10, 15800000, 0, 430000, 410000, 490000, 520000, 650000, 720000, 550000, 15800000.00, 530, 29811.32, 1, 4, 0, NULL, 55.00, 95.00, 12, 4.4, 240, 82.10, 10.20, 24.50, 29.00, 26.30, 21.80, 23.50, 27.10, 15800000, 22.50, 4.5, 14900000, 21.80, 4.4, 'FILED', '2026-05-09', false, 'PAID', 0, 0, 0, 0, 4, true, false, false, NOW(), NOW()),
('1045678901', 'I56192', '비알코올음료점업', 'SEOUL-JR', '2026-05-01', 12, 96000000, 7800000, 7600000, 1600000, 8300000, 20.51, -5.90, 4900000, 2900000, 210000, 200000, 230000, 250000, 310000, 360000, 250000, 7800000.00, 410, 19024.39, 4, 8, 160, 4.1, 24.00, 71.00, 3, 4.2, 170, 73.50, 18.20, 58.00, 61.20, 60.10, 55.30, 58.80, 61.50, 9800000, 24.00, 4.4, 8900000, 23.10, 4.2, 'FILED', '2026-05-12', false, 'PAID', 3, 48000000, 4000000, 42000000, 2, true, false, true, NOW(), NOW()),
('1056789012', 'I55101', '숙박업', 'SEOUL-SC', '2026-05-01', 48, 150000000, 12400000, 10100000, 4100000, 11800000, 33.06, 5.13, 12400000, 0, 280000, 270000, 360000, 380000, 520000, 680000, 490000, 12400000.00, 360, 34444.44, 1, 3, 0, NULL, 31.00, 88.00, 5, 4.8, 290, 91.30, 4.80, 28.00, 24.00, 25.50, 20.30, 22.10, 24.80, 11200000, 30.50, 4.6, 10800000, 29.80, 4.5, 'FILED', '2026-05-08', false, 'PAID', 1, 24000000, 2000000, 18000000, 3, true, true, true, NOW(), NOW()),
('1067890123', 'I56192', '비알코올음료점업', 'GYEONGGI-SN', '2026-05-01', 30, 210000000, 18800000, 16200000, 4700000, 17600000, 25.00, 5.61, 9200000, 9600000, 510000, 490000, 560000, 590000, 740000, 850000, 610000, 18800000.00, 760, 24736.84, 1, 4, 430, 4.4, 36.00, 84.00, 4, 4.5, 420, 84.70, 9.10, 22.00, 27.50, 24.80, 19.50, 21.30, 25.60, 9800000, 24.00, 4.4, 17200000, 23.20, 4.3, 'FILED', '2026-05-10', false, 'PAID', 2, 42000000, 3500000, 35000000, 4, true, true, false, NOW(), NOW()),
('1078901234', 'I56221', '음식점업(치킨)', 'SEOUL-DDM', '2026-05-01', 60, 168000000, 13200000, 12500000, 3300000, 14100000, 25.00, -5.71, 13200000, 0, 310000, 300000, 390000, 420000, 580000, 650000, 480000, 13200000.00, 290, 45517.24, 3, 9, 0, NULL, 27.00, 78.00, 2, 4.3, 130, 76.40, 14.30, 41.00, 39.50, 40.20, 35.80, 38.10, 40.90, 12100000, 23.80, 4.2, 12400000, 22.90, 4.1, 'FILED', '2026-05-13', false, 'PAID', 4, 60000000, 5000000, 52000000, 2, true, false, true, NOW(), NOW()),
('1089012345', 'I56192', '비알코올음료점업', 'SEOUL-YD', '2026-05-01', 42, 192000000, 16700000, 13900000, 5100000, 15100000, 30.54, 6.25, 13600000, 3100000, 460000, 440000, 510000, 540000, 670000, 780000, 560000, 16700000.00, 590, 28305.08, 1, 2, 120, 4.7, 33.00, 90.00, 7, 4.8, 360, 89.60, 5.20, 20.00, 19.00, 19.50, 15.80, 17.20, 20.30, 9800000, 24.00, 4.4, 15300000, 23.50, 4.3, 'FILED', '2026-05-09', false, 'PAID', 1, 18000000, 1500000, 12000000, 6, true, true, true, NOW(), NOW()),
('1090123456', 'I56111', '음식점업(한식)', 'SEOUL-GD', '2026-05-01', 15, 108000000, 9200000, 6900000, 3600000, 8100000, 39.13, 12.50, 9200000, 0, 250000, 240000, 300000, 320000, 400000, 460000, 340000, 9200000.00, 95, 96842.11, 5, 12, 0, NULL, 18.00, 83.00, 10, 4.6, 75, 86.00, 7.80, 36.00, 30.50, 33.20, 28.90, 31.10, 34.80, 15800000, 22.50, 4.5, 8600000, 21.30, 4.3, 'FILED', '2026-05-14', false, 'PAID', 0, 0, 0, 0, 1, true, false, false, NOW(), NOW()),
('1101234567', 'I56111', '음식점업(한식)', 'SEOUL-NW', '2026-05-01', 72, 264000000, 22600000, 18100000, 7900000, 21400000, 34.96, 3.95, 22600000, 0, 620000, 590000, 700000, 740000, 920000, 1060000, 780000, 22600000.00, 410, 55121.95, 1, 3, 0, NULL, 22.00, 87.00, 6, 4.7, 220, 88.90, 5.90, 16.00, 15.50, 15.80, 12.30, 13.90, 16.50, 15800000, 22.50, 4.5, 20800000, 32.10, 4.5, 'FILED', '2026-05-07', false, 'PAID', 2, 54000000, 4500000, 45000000, 8, true, true, true, NOW(), NOW());

-- business_number='1012345678' (음식점업 분식, SEOUL-GN) 추가 5개월치
INSERT INTO my_biz_data (business_number, industry_code, industry_name, district_code, reference_month, business_age_months, annual_income, monthly_revenue, monthly_outflow, estimated_profit, prev_month_revenue, monthly_profit_rate, monthly_profit_growth_rate, pos_sales_amount, delivery_sales_amount, avg_revenue_mon, avg_revenue_tue, avg_revenue_wed, avg_revenue_thu, avg_revenue_fri, avg_revenue_sat, avg_revenue_sun, monthly_payment_amount, monthly_payment_count, avg_payment_amount, days_since_last_transaction, max_inactive_days, delivery_order_count, delivery_rating, online_reorder_rate, online_reply_rate, online_info_update_count, review_rating, review_count, positive_review_ratio, negative_review_ratio, industry_sales_rank, industry_profit_rank, industry_satisfaction_rank, district_sales_rank, district_profit_rank, district_satisfaction_rank, industry_avg_revenue, industry_avg_profit_rate, industry_avg_review_rating, district_avg_revenue, district_avg_profit_rate, district_avg_review_rating, vat_filing_status, vat_filing_date, tax_overdue, insurance_payment_status, existing_loan_count, annual_repayment, monthly_repayment, total_loan_balance, employee_count, has_sns, has_online_reservation, is_near_subway, created_at, updated_at) VALUES
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2025-12-01', 31, 240000000, 16800000, 15100000, 4100000, 16200000, 24.40, 4.60, 10700000, 6100000, 460000, 440000, 500000, 530000, 660000, 760000, 580000, 16800000.00, 710, 23661.97, 1, 3, 270, 4.5, 35.00, 88.00, 4, 4.6, 440, 86.10, 7.20, 22.50, 25.00, 23.40, 18.90, 21.10, 24.30, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2025-12-10', false, 'PAID', 2, 36000000, 3000000, 28000000, 5, true, true, true, NOW(), NOW()),
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-01-01', 32, 240000000, 17400000, 15600000, 4300000, 16800000, 24.71, 4.88, 11000000, 6400000, 480000, 455000, 515000, 550000, 680000, 790000, 600000, 17400000.00, 730, 23835.62, 1, 3, 285, 4.5, 36.00, 89.00, 5, 4.6, 460, 86.50, 7.10, 21.50, 24.00, 22.80, 18.30, 20.50, 23.70, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-01-10', false, 'PAID', 2, 36000000, 3000000, 28000000, 5, true, true, true, NOW(), NOW()),
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-02-01', 33, 240000000, 18100000, 16100000, 4500000, 17400000, 24.86, 4.65, 11400000, 6700000, 500000, 475000, 535000, 570000, 710000, 820000, 620000, 18100000.00, 755, 23973.51, 1, 3, 300, 4.6, 36.50, 90.00, 5, 4.7, 475, 87.00, 6.90, 21.00, 23.00, 22.10, 17.80, 19.90, 23.10, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-02-10', false, 'PAID', 2, 36000000, 3000000, 28000000, 5, true, true, true, NOW(), NOW()),
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-03-01', 34, 240000000, 19000000, 17000000, 4700000, 18100000, 24.74, 4.44, 11900000, 7100000, 520000, 495000, 560000, 595000, 740000, 860000, 650000, 19000000.00, 780, 24358.97, 1, 3, 315, 4.6, 37.00, 91.00, 6, 4.7, 490, 87.50, 6.80, 20.00, 22.00, 21.30, 17.10, 19.20, 22.40, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-03-10', false, 'PAID', 2, 36000000, 3000000, 28000000, 5, true, true, true, NOW(), NOW()),
('1012345678', 'I56111', '음식점업(분식)', 'SEOUL-GN', '2026-04-01', 35, 240000000, 19500000, 17800000, 4900000, 19000000, 25.13, 4.26, 12100000, 7400000, 535000, 510000, 575000, 610000, 760000, 880000, 665000, 19500000.00, 800, 24375.00, 1, 3, 328, 4.6, 37.50, 91.00, 6, 4.7, 500, 87.80, 6.70, 19.50, 21.50, 20.80, 16.60, 18.70, 21.90, 15800000, 22.50, 4.5, 16200000, 21.80, 4.4, 'FILED', '2026-04-10', false, 'PAID', 2, 36000000, 3000000, 28000000, 5, true, true, true, NOW(), NOW());

-- =====================================================
-- 4. s_grade_feature
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
(1,  36, 11.54, 28.85, 1.75, 21000000.0, 19950000.0, 18900000.0, 1, 3,  70.2, 2.31,  2.54,  5.77,  0.3206, 0.3526, 0.8014, 98.0,  TRUE,  45.0, FALSE, 'GROWING',  'GROWING',  4.7, 510, 4.6,  340, 88.2, TRUE,  4, 5, TRUE,  NOW()),
-- 이서아 | 비알코올음료점업 | 업종평균 800만 | ratio=1.44
(2,  18, 9.72,  24.30, 1.44, 11500000.0, 10925000.0, 10350000.0, 2, 5,  72.4, 3.24,  3.56,  8.10,  0.5400, 0.5940, 1.3500, 100.0, TRUE,  55.0, FALSE, 'GROWING',  'STABLE',   4.6, 380, 4.5,  210, 85.4, TRUE,  2, 3, TRUE,  NOW()),
-- 박지훈 | 음식점업(한식) | 업종평균 1200만 | ratio=1.32
(3,  24, 9.06,  22.65, 1.32, 15800000.0, 15010000.0, 14220000.0, 1, 4,  80.0, 2.27,  2.49,  5.66,  0.3775, 0.4153, 0.9437, 75.0,  FALSE, 60.0, FALSE, 'STABLE',   'GROWING',  4.4, 240, NULL, 0,   82.1, FALSE, 3, 4, TRUE,  NOW()),
-- 최유진 | 비알코올음료점업 | 업종평균 800만 | ratio=0.97
(4,  12, -9.03, -22.57,0.97, 7800000.0,  7410000.0,  7020000.0,  4, 8,  47.0, -4.51, -4.97, -11.29,-0.7525,-0.8277,-1.8808,64.0,  TRUE,  70.0, FALSE, 'STABLE',   'STABLE',   4.2, 170, 4.1,  160, 73.5, FALSE, 2, 2, TRUE,  NOW()),
-- 정우진 | 숙박업 | 업종평균 1000만 | ratio=1.24
(5,  48, 7.62,  19.05, 1.24, 12400000.0, 11780000.0, 11160000.0, 1, 3,  62.6, 2.54,  2.79,  6.35,  0.1588, 0.1746, 0.3969, 95.0,  TRUE,  50.0, FALSE, 'GROWING',  'GROWING',  4.8, 290, NULL, 0,   91.3, TRUE,  5, 3, TRUE,  NOW()),
-- 김나은 | 비알코올음료점업 | 업종평균 800만 | ratio=2.35
(6,  30, 10.23, 25.58, 2.35, 18800000.0, 17860000.0, 16920000.0, 1, 4,  60.0, 2.56,  2.81,  6.39,  0.3410, 0.3751, 0.8527, 92.0,  FALSE, 65.0, FALSE, 'STABLE',   'STABLE',   4.5, 420, 4.4,  430, 84.7, TRUE,  3, 4, TRUE,  NOW()),
-- 윤태현 | 음식점업(치킨) | 업종평균 1400만 | ratio=0.94
(7,  60, -9.57, -23.93,0.94, 13200000.0, 12540000.0, 11880000.0, 3, 9,  48.0, -4.79, -5.26, -11.96,-0.1595,-0.1755,-0.3988,61.0,  TRUE,  75.0, FALSE, 'DECLINING','DECLINING',4.3, 130, NULL, 0,   76.4, FALSE, 6, 2, TRUE,  NOW()),
-- 송하린 | 비알코올음료점업 | 업종평균 800만 | ratio=2.09
(8,  42, 15.90, 39.75, 2.09, 16700000.0, 15865000.0, 15030000.0, 1, 2,  69.2, 2.65,  2.92,  6.62,  0.3786, 0.4164, 0.9464, 100.0, TRUE,  55.0, FALSE, 'GROWING',  'GROWING',  4.8, 360, 4.7,  120, 89.6, TRUE,  4, 6, TRUE,  NOW()),
-- 장도현 | 음식점업(한식) | 업종평균 1200만 | ratio=0.77
(9,  15, 20.37, 50.93, 0.77, 9200000.0,  8740000.0,  8280000.0,  5, 12, 60.4, 20.37, 22.41, 50.93, 1.3580, 1.4938, 3.3953, 75.0,  FALSE, 40.0, FALSE, 'GROWING',  'GROWING',  4.6, 75,  NULL, 0,   86.0, FALSE, 2, 1, TRUE,  NOW()),
-- 임수빈 | 음식점업(한식) | 업종평균 1200만 | ratio=1.88
(10, 72, 8.42,  21.05, 1.88, 22600000.0, 21470000.0, 20340000.0, 1, 3,  61.6, 1.05,  1.16,  2.63,  0.1169, 0.1286, 0.2924, 98.0,  TRUE,  60.0, FALSE, 'STABLE',   'STABLE',   4.7, 220, NULL, 0,   88.9, TRUE,  7, 8, TRUE,  NOW());