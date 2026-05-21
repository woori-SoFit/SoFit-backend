-- ADMIN 시드 데이터 (회원가입 플로우 없이 하드코딩)
-- 비밀번호: admin1234! (BCrypt 해시)

INSERT IGNORE INTO users (login_id, password_hash, name, phone_number, resident_number, role, status, created_at, updated_at)
VALUES
    ('teller01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '김은행', '01011111111', '9001011', 'ADMIN_BANK_TELLER', 'ACTIVE', NOW(), NOW()),
    ('manager01', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '박지점장', '01022222222', '8501012', 'ADMIN_BANK_MANAGER', 'ACTIVE', NOW(), NOW()),
    ('devadmin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '이개발', '01033333333', '9201013', 'ADMIN_DEV', 'ACTIVE', NOW(), NOW());
