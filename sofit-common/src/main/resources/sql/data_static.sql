-- =====================================================
-- SoFit Static Data (서비스 고정 데이터)
-- 실행 순서: 1번째
-- DDL → term → s_scoring_rule → loan_product → loan_product_options
-- =====================================================


-- =====================================================
-- DDL: s_grade_feature
-- =====================================================
CREATE TABLE IF NOT EXISTS s_grade_feature (
    feature_id                          BIGINT       NOT NULL AUTO_INCREMENT,
    biz_data_id                         BIGINT       NOT NULL,
    user_id                             BIGINT       NOT NULL,
    business_age_months                 INT,
    quarterly_revenue_growth_rate       DECIMAL(8,4),
    annual_revenue_growth_rate          DECIMAL(8,4),
    revenue_vs_industry_avg_ratio       DECIMAL(8,4),
    avg_monthly_transaction_3m          DECIMAL(15,2),
    avg_monthly_transaction_6m          DECIMAL(15,2),
    avg_monthly_transaction_12m         DECIMAL(15,2),
    days_since_last_transaction         INT,
    max_inactive_days                   INT,
    online_platform_activity_index      DECIMAL(8,4),
    revenue_growth_per_employee_3m      DECIMAL(8,4),
    revenue_growth_per_employee_6m      DECIMAL(8,4),
    revenue_growth_per_employee_12m     DECIMAL(8,4),
    revenue_growth_per_business_age_3m  DECIMAL(8,4),
    revenue_growth_per_business_age_6m  DECIMAL(8,4),
    revenue_growth_per_business_age_12m DECIMAL(8,4),
    online_accessibility_score          DECIMAL(8,4),
    is_near_subway                      TINYINT(1)   DEFAULT 0,
    commercial_saturation_score         DECIMAL(8,4),
    is_traditional_market               TINYINT(1)   DEFAULT 0,
    commercial_trend                    ENUM('GROWING','STABLE','DECLINING'),
    industry_trend                      ENUM('GROWING','STABLE','DECLINING'),
    review_rating                       DECIMAL(3,1),
    review_count                        INT,
    delivery_rating                     DECIMAL(3,1),
    delivery_order_count                INT,
    positive_review_ratio               DECIMAL(8,4),
    has_online_reservation              TINYINT(1)   DEFAULT 0,
    owner_experience_years              INT,
    employee_count                      INT,
    has_sns                             TINYINT(1)   DEFAULT 0,
    created_at                          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (feature_id),
    INDEX idx_s_grade_feature_user_created (user_id, created_at),
    INDEX idx_s_grade_feature_biz_data (biz_data_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


-- =====================================================
-- 1. term (약관)
-- =====================================================
SET NAMES utf8mb4;

INSERT INTO term (
    `term_id`,`term_type`,`version`,`title`,`file_url`,`is_active`,`is_required`,`effective_at`,`created_at`,`updated_at`
) VALUES
      (1,  'PERSONAL_INFO',     'v1.0', '전자금융거래 기본약관',                              '/terms/term_1.pdf',  1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (2,  'PERSONAL_INFO',     'v1.0', '개인정보 수집·이용 동의서 (회원가입)',                '/terms/term_2.pdf',  1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (3,  'PERSONAL_INFO',     'v1.0', '고유식별정보 처리 동의서',                           '/terms/term_3.pdf',  1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (4,  'PERSONAL_INFO',     'v1.0', '마케팅·광고 목적 개인정보 수집·이용 동의서',          '/terms/term_4.pdf',  1, 0, '2026-05-12 00:00:00', NOW(), NOW()),
      (5,  'PERSONAL_INFO',     'v1.0', '개인정보 제3자 제공 동의서 (마케팅 제휴사)',           '/terms/term_5.pdf',  1, 0, '2026-05-12 00:00:00', NOW(), NOW()),
      (6,  'MYDATA',            'v1.0', '마이데이터 서비스 이용약관',                          '/terms/term_6.pdf',  1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (7,  'MYDATA',            'v1.0', '개인(신용)정보 수집·이용 동의서 (마이데이터)',         '/terms/term_7.pdf',  1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (8,  'MYDATA',            'v1.0', '개인(신용)정보 제3자 제공 동의서 (마이데이터)',        '/terms/term_8.pdf',  1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (9,  'MYDATA',            'v1.0', '본인신용정보 전송 요구서',                            '/terms/term_9.pdf',  1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (10, 'MYBIZDATA',         'v1.0', '개인사업자 신용정보 수집·이용 동의서',                '/terms/term_10.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (11, 'MYBIZDATA',         'v1.0', '국세청 과세정보 제공 동의서',                         '/terms/term_11.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (12, 'MYBIZDATA',         'v1.0', '개인(신용)정보 제3자 제공 동의서 (공공 마이데이터)',   '/terms/term_12.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (13, 'MYBIZDATA',         'v1.0', '카드사 매출 데이터 제공 동의서',                      '/terms/term_13.pdf', 1, 0, '2026-05-12 00:00:00', NOW(), NOW()),
      (14, 'LOAN_APPLICATION',  'v1.0', '개인(신용)정보 수집·이용 동의서 (여신거래)',           '/terms/term_14.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (15, 'LOAN_APPLICATION',  'v1.0', '개인(신용)정보 조회 동의서 (신용조회회사)',            '/terms/term_15.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (16, 'LOAN_APPLICATION',  'v1.0', '개인(신용)정보 조회 동의서 (신용정보집중기관)',        '/terms/term_16.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (17, 'LOAN_APPLICATION',  'v1.0', '개인(신용)정보 제3자 제공 동의서 (여신)',              '/terms/term_17.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (18, 'LOAN_APPLICATION',  'v1.0', '금융거래 목적 확인서',                               '/terms/term_18.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (19, 'LOAN_AGREEMENT',    'v1.0', '은행 여신거래 기본약관 (기업용)',                     '/terms/term_19.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (20, 'LOAN_AGREEMENT',    'v1.0', '여신거래약정서 (기업용)',                             '/terms/term_20.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (21, 'LOAN_AGREEMENT',    'v1.0', '상품설명서 교부 확인서',                              '/terms/term_21.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (22, 'LOAN_AGREEMENT',    'v1.0', '개인(신용)정보 수집·이용 동의서 (약정)',               '/terms/term_22.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW()),
      (23, 'LOAN_AGREEMENT',    'v1.0', '대출계약 철회권 안내 확인서',                         '/terms/term_23.pdf', 1, 1, '2026-05-12 00:00:00', NOW(), NOW());

-- =====================================================
-- 2. s_scoring_rule (S등급 가산점)
-- =====================================================
INSERT INTO s_scoring_rule (
    `grade`,`score_addition`,`description`,`created_at`,`updated_at`
) VALUES
('S1',  100, '혁신적 성장성',      '2026-05-28 06:29:51', '2026-05-28 06:29:51'),
('S2',  80,  '초고속 성장성',      '2026-05-28 06:29:51', '2026-05-28 06:29:51'),
('S3',  65,  '고속 성장성',        '2026-05-28 06:29:51', '2026-05-28 06:29:51'),
('S4',  50,  '견고한 성장세',      '2026-05-28 06:29:51', '2026-05-28 06:29:51'),
('S5',  35,  '안정적 성장세',      '2026-05-28 06:29:51', '2026-05-28 06:29:51'),
('S6',  20,  '완만한 성장세',      '2026-05-28 06:29:51', '2026-05-28 06:29:51'),
('S7',  10,  '성장 정체기 시작',   '2026-05-28 06:29:51', '2026-05-28 06:29:51'),
('S8',  5,   '최소 성장 지표 확인','2026-05-28 06:29:51', '2026-05-28 06:29:51'),
('S9',  0,   '성장 지표 미비',     '2026-05-28 06:29:51', '2026-05-28 06:29:51'),
('S10', 0,   '성장 지표 없음',     '2026-05-28 06:29:51', '2026-05-28 06:29:51');

-- =====================================================
-- 3. loan_product (대출 상품)
-- =====================================================
INSERT INTO loan_product (
    product_id, product_name, description, target_description, status,
    min_rate, max_rate, min_limit, max_limit, min_term, max_term,
    title, subtitle, industry_type,
    min_business_age_months, annual_income_limit, income_type_code_limit,
    credit_score_limit, existing_loan_amt_limit,
    created_at, updated_at
) VALUES
(1, '우리 사장님 대출', '우리 사장님 곁을 든든하게!',
 '우리은행 내부 신용등급 BBB-(SOHO 5)등급 이상 개인사업자', 'ACTIVE',
 5.46, 14.00, 5000000, 100000000, 12, 240,
 '사장님을 위한 든든한 대출', '개인사업자 맞춤형 우대금리 지원 대출상품',
 NULL,
 12, 10000000, 'BUSINESS',
 700, 500000000,
 NOW(), NOW()),
(2, '우리 Oh!(5) 클릭 대출', '개인사업자 전용 비대면 대출상품',
 '사업기간 1년 이상 개인사업자', 'ACTIVE',
 5.74, 14.00, 1000000, 30000000, 12, 60,
 '빠르고 간편한 사업자 대출', '개인사업자 신속·초간편 비대면 대출상품',
 NULL,
 12, 6000000, 'BUSINESS',
 621, 200000000,
 NOW(), NOW());

-- =====================================================
-- 4. loan_product_options (대출 상품 옵션)
-- =====================================================
INSERT INTO loan_product_option (
      product_id, purpose, repayment_method, max_term_months, created_at, updated_at
) VALUES
(1, 'WORKING_CAPITAL',  'BULLET',          12, NOW(), NOW()),
(1, 'WORKING_CAPITAL',  'EQUAL_PRINCIPAL',  60, NOW(), NOW()),
(1, 'WORKING_CAPITAL',  'EQUAL_PAYMENT',    60, NOW(), NOW()),
(1, 'FACILITY_CAPITAL', 'BULLET',           36, NOW(), NOW()),
(1, 'FACILITY_CAPITAL', 'EQUAL_PRINCIPAL', 240, NOW(), NOW()),
(1, 'FACILITY_CAPITAL', 'EQUAL_PAYMENT',   240, NOW(), NOW()),
(2, 'WORKING_CAPITAL',  'BULLET',           12, NOW(), NOW()),
(2, 'WORKING_CAPITAL',  'EQUAL_PRINCIPAL',  60, NOW(), NOW()),
(2, 'WORKING_CAPITAL',  'EQUAL_PAYMENT',    60, NOW(), NOW());



-- =====================================================
-- 5. loan_rate_policy (금리/한도 정책)
-- =====================================================
INSERT INTO loan_rate_policy (
    product_id, min_score, max_score, interest_rate, max_limit, created_at, updated_at
) VALUES
(1, 700,  720,  14.00, 30000000.00,  NOW(), NOW()),
(1, 720,  750,  11.50, 50000000.00,  NOW(), NOW()),
(1, 750,  780,  9.00,  70000000.00,  NOW(), NOW()),
(1, 780,  850,  7.20,  85000000.00,  NOW(), NOW()),
(1, 850,  1001, 5.46,  100000000.00, NOW(), NOW()),
(2, 621,  651,  14.00, 5000000.00,   NOW(), NOW()),
(2, 651,  681,  12.00, 10000000.00,  NOW(), NOW()),
(2, 681,  711,  10.00, 15000000.00,  NOW(), NOW()),
(2, 711,  761,  8.50,  20000000.00,  NOW(), NOW()),
(2, 761,  791,  7.00,  25000000.00,  NOW(), NOW()),
(2, 791,  1001, 5.74,  30000000.00,  NOW(), NOW());