-- ADMIN 시드 데이터 (회원가입 플로우 없이 하드코딩)
-- 비밀번호: admin1234! (BCrypt 해시)

INSERT IGNORE INTO users (login_id, password_hash, name, phone_number, resident_number, role, status, created_at, updated_at)
VALUES
    ('teller01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김은행', '01011111111', '9001011', 'ADMIN_BANK_TELLER', 'ACTIVE', NOW(), NOW()),
    ('manager01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '박지점장', '01022222222', '8501012', 'ADMIN_BANK_MANAGER', 'ACTIVE', NOW(), NOW()),
    ('devadmin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '이개발', '01033333333', '9201013', 'ADMIN_DEV', 'ACTIVE', NOW(), NOW());

-- ============================================================
-- 약관 시드 데이터 (우리은행 소상공인 대출 플랫폼 SoFit 기준)
-- 버전 v1.0 / 시행일 2026-05-12
-- ============================================================

-- 기존 약관 데이터 초기화 (재시작 시 중복 방지)
DELETE FROM consent_history;
DELETE FROM term;

-- ----------------------------------------------------------------
-- PERSONAL_INFO : 회원가입 약관 묶음
-- PDF: /terms/personal_info_v1.0.pdf
-- ----------------------------------------------------------------
INSERT INTO term (term_type, version, title, file_url, is_active, is_required, effective_at, created_at)
VALUES
    ('PERSONAL_INFO', 'v1.0', '전자금융거래 기본약관',
     '/terms/personal_info_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('PERSONAL_INFO', 'v1.0', '개인정보 수집·이용 동의서 (회원가입)',
     '/terms/personal_info_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('PERSONAL_INFO', 'v1.0', '고유식별정보 처리 동의서',
     '/terms/personal_info_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('PERSONAL_INFO', 'v1.0', '마케팅·광고 목적 개인정보 수집·이용 동의서',
     '/terms/personal_info_v1.0.pdf', TRUE, FALSE, '2026-05-12 00:00:00', NOW()),

    ('PERSONAL_INFO', 'v1.0', '개인정보 제3자 제공 동의서 (마케팅 제휴사)',
     '/terms/personal_info_v1.0.pdf', TRUE, FALSE, '2026-05-12 00:00:00', NOW());

-- ----------------------------------------------------------------
-- MYDATA : 마이데이터 연동 약관 묶음
-- PDF: /terms/mydata_v1.0.pdf
-- ----------------------------------------------------------------
INSERT INTO term (term_type, version, title, file_url, is_active, is_required, effective_at, created_at)
VALUES
    ('MYDATA', 'v1.0', '마이데이터 서비스 이용약관',
     '/terms/mydata_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('MYDATA', 'v1.0', '개인(신용)정보 수집·이용 동의서 (마이데이터)',
     '/terms/mydata_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('MYDATA', 'v1.0', '개인(신용)정보 제3자 제공 동의서 (마이데이터)',
     '/terms/mydata_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('MYDATA', 'v1.0', '본인신용정보 전송 요구서',
     '/terms/mydata_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW());

-- ----------------------------------------------------------------
-- MYBIZDATA : 마이비즈데이터 연동 약관 묶음
-- PDF: /terms/mybizdata_v1.0.pdf
-- ----------------------------------------------------------------
INSERT INTO term (term_type, version, title, file_url, is_active, is_required, effective_at, created_at)
VALUES
    ('MYBIZDATA', 'v1.0', '개인사업자 신용정보 수집·이용 동의서',
     '/terms/mybizdata_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('MYBIZDATA', 'v1.0', '국세청 과세정보 제공 동의서',
     '/terms/mybizdata_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('MYBIZDATA', 'v1.0', '개인(신용)정보 제3자 제공 동의서 (공공 마이데이터)',
     '/terms/mybizdata_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('MYBIZDATA', 'v1.0', '카드사 매출 데이터 제공 동의서',
     '/terms/mybizdata_v1.0.pdf', TRUE, FALSE, '2026-05-12 00:00:00', NOW());

-- ----------------------------------------------------------------
-- LOAN_APPLICATION : 대출 신청 약관 묶음
-- PDF: /terms/loan_application_v1.0.pdf
-- ----------------------------------------------------------------
INSERT INTO term (term_type, version, title, file_url, is_active, is_required, effective_at, created_at)
VALUES
    ('LOAN_APPLICATION', 'v1.0', '개인(신용)정보 수집·이용 동의서 (여신거래)',
     '/terms/loan_application_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('LOAN_APPLICATION', 'v1.0', '개인(신용)정보 조회 동의서 (신용조회회사)',
     '/terms/loan_application_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('LOAN_APPLICATION', 'v1.0', '개인(신용)정보 조회 동의서 (신용정보집중기관)',
     '/terms/loan_application_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('LOAN_APPLICATION', 'v1.0', '개인(신용)정보 제3자 제공 동의서 (여신)',
     '/terms/loan_application_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('LOAN_APPLICATION', 'v1.0', '금융거래 목적 확인서',
     '/terms/loan_application_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW());

-- ----------------------------------------------------------------
-- LOAN_AGREEMENT : 약정 체결 약관 묶음
-- PDF: /terms/loan_agreement_v1.0.pdf
-- ----------------------------------------------------------------
INSERT INTO term (term_type, version, title, file_url, is_active, is_required, effective_at, created_at)
VALUES
    ('LOAN_AGREEMENT', 'v1.0', '은행 여신거래 기본약관 (기업용)',
     '/terms/loan_agreement_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('LOAN_AGREEMENT', 'v1.0', '여신거래약정서 (기업용)',
     '/terms/loan_agreement_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('LOAN_AGREEMENT', 'v1.0', '상품설명서 교부 확인서',
     '/terms/loan_agreement_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('LOAN_AGREEMENT', 'v1.0', '개인(신용)정보 수집·이용 동의서 (약정)',
     '/terms/loan_agreement_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW()),

    ('LOAN_AGREEMENT', 'v1.0', '대출계약 철회권 안내 확인서',
     '/terms/loan_agreement_v1.0.pdf', TRUE, TRUE, '2026-05-12 00:00:00', NOW());
