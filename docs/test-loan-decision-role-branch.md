# 대출 심사 승인/거절 역할 분기 테스트

## 1. 테스트 데이터 SQL

```sql
-- ================================================
-- 테스트 사용자 (비밀번호: test1234 → BCrypt)
-- ================================================

-- 일반 고객 (USER)
INSERT INTO users (login_id, password_hash, name, phone_number, resident_number, role, status, created_at, updated_at)
VALUES ('test_user', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트고객', '01012345678', '9501011', 'USER', 'ACTIVE', NOW(), NOW());

-- 행원 (ADMIN_BANK_TELLER)
INSERT INTO users (login_id, password_hash, name, phone_number, resident_number, role, status, created_at, updated_at)
VALUES ('test_teller', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트행원', '01011112222', '8801011', 'ADMIN_BANK_TELLER', 'ACTIVE', NOW(), NOW());

-- 지점장 (ADMIN_BANK_MANAGER)
INSERT INTO users (login_id, password_hash, name, phone_number, resident_number, role, status, created_at, updated_at)
VALUES ('test_manager', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '테스트지점장', '01033334444', '8201011', 'ADMIN_BANK_MANAGER', 'ACTIVE', NOW(), NOW());

-- ================================================
-- 대출 상품
-- ================================================
INSERT INTO loan_product (product_name, description, status, min_rate, max_rate, min_limit, max_limit, min_term, max_term, created_at, updated_at)
VALUES ('소상공인 성장 대출', '소상공인 대상 성장 S등급 기반 대출', 'ACTIVE', 3.50, 8.50, 5000000, 100000000, 12, 60, NOW(), NOW());

-- ================================================
-- 대출 신청 건 (SYSTEM_APPROVED 상태 — 행원이 처리할 건)
-- user_id, product_id는 위에서 INSERT된 ID에 맞게 조정하세요
-- ================================================

-- 케이스 1: SYSTEM_APPROVED 상태 + 행원에게 배정 → 행원 승인 테스트용
INSERT INTO loan_application (user_id, product_id, requested_amount, requested_term, repayment_method, purpose, status, applied_at, assigned_banker_id, created_at, updated_at)
VALUES (
    (SELECT user_id FROM users WHERE login_id = 'min'),
    (SELECT product_id FROM loan_product WHERE product_id=1 LIMIT 1),
    30000000, 36, 'EQUAL_PAYMENT', 'OPERATING_FUNDS', 'SYSTEM_APPROVED', NOW(), 
    (SELECT user_id FROM users WHERE login_id = 'min_teller'),
    NOW(), NOW()
);

-- 케이스 2: SYSTEM_APPROVED 상태 + 행원에게 배정 → 행원 거절 테스트용
INSERT INTO loan_application (user_id, product_id, requested_amount, requested_term, repayment_method, purpose, status, applied_at, assigned_banker_id, created_at, updated_at)
VALUES (
    (SELECT user_id FROM users WHERE login_id = 'min'),
    (SELECT product_id FROM loan_product WHERE product_id=1),
    50000000, 48, 'BULLET', 'OPERATING_FUNDS', 'SYSTEM_APPROVED', NOW(),
    (SELECT user_id FROM users WHERE login_id = 'min_teller'),
    NOW(), NOW()
);

-- 케이스 3: MANAGER_REVIEW 상태 + 지점장에게 배정 → 지점장 승인 테스트용
INSERT INTO loan_application (user_id, product_id, requested_amount, requested_term, repayment_method, purpose, status, applied_at, assigned_banker_id, created_at, updated_at)
VALUES (
    (SELECT user_id FROM users WHERE login_id = 'min_test'),
    (SELECT product_id FROM loan_product WHERE product_id=2),
    20000000, 24, 'EQUAL_PRINCIPAL', 'OPERATING_FUNDS', 'MANAGER_REVIEW', NOW(),
    (SELECT user_id FROM users WHERE login_id = 'min_manager'),
    NOW(), NOW()
);

-- 케이스 4: MANAGER_REVIEW 상태 + 지점장에게 배정 → 지점장 거절 테스트용
INSERT INTO loan_application (user_id, product_id, requested_amount, requested_term, repayment_method, purpose, status, applied_at, assigned_banker_id, created_at, updated_at)
VALUES (
    (SELECT user_id FROM users WHERE login_id = 'min_test'),
    (SELECT product_id FROM loan_product WHERE product_id=2),
    80000000, 60, 'EQUAL_PAYMENT', 'OPERATING_FUNDS', 'MANAGER_REVIEW', NOW(),
    (SELECT user_id FROM users WHERE login_id = 'min_manager'),
    NOW(), NOW()
);

-- ================================================
-- 확인용 쿼리
-- ================================================
SELECT application_id, status, assigned_banker_id FROM loan_application ORDER BY application_id DESC LIMIT 4;
SELECT user_id, login_id, role FROM users WHERE login_id IN ('min', 'min_test', 'min_teller', 'min_manager');
```

> ⚠️ 이미 테스트 데이터가 있다면 users/loan_product INSERT는 스킵하고, `assigned_banker_id`와 `user_id`만 실제 ID로 교체하세요.

---

## 2. API Dog 테스트 시나리오

### 사전 준비
- 행원(`test_teller`)과 지점장(`test_manager`)으로 각각 로그인하여 세션 쿠키 확보
- 위 SQL 실행 후 생성된 `application_id`를 확인

---

### 케이스 1: 행원(TELLER)이 승인

**로그인**: `test_teller` 세션 사용

```
POST /api/admin/loan-applications/{케이스1_applicationId}/approve
Content-Type: application/json

{
  "approvedAmount": 25000000,
  "approvedRate": 5.50,
  "approvedTerm": 36,
  "repaymentMethod": "EQUAL_PAYMENT",
  "comment": "행원 1차 승인 의견"
}
```

**기대 결과**:
- 응답: `isSuccess: true`, `decision: APPROVED`
- DB 확인:
  - `loan_application.status` = `MANAGER_REVIEW` (APPROVED 아님!)
  - `loan_decision` 레코드 생성 (`decision = APPROVED`, `created_by = 행원 userId`)
  - `notification` 테이블에 **신규 레코드 없음**

---

### 케이스 2: 행원(TELLER)이 거절

**로그인**: `test_teller` 세션 사용

```
POST /api/admin/loan-applications/{케이스2_applicationId}/reject
Content-Type: application/json

{
  "comment": "신용 점수 미달로 거절 의견 제출"
}
```

**기대 결과**:
- 응답: `isSuccess: true`, `decision: REJECTED`
- DB 확인:
  - `loan_application.status` = `MANAGER_REVIEW` (REJECTED 아님!)
  - `loan_decision` 레코드 생성 (`decision = REJECTED`, `created_by = 행원 userId`)
  - `notification` 테이블에 **신규 레코드 없음**

---

### 케이스 3: 지점장(MANAGER)이 승인

**로그인**: `test_manager` 세션 사용

```
POST /api/admin/loan-applications/{케이스3_applicationId}/approve
Content-Type: application/json

{
  "approvedAmount": 20000000,
  "approvedRate": 4.80,
  "approvedTerm": 24,
  "repaymentMethod": "EQUAL_PRINCIPAL",
  "comment": "지점장 최종 승인"
}
```

**기대 결과**:
- 응답: `isSuccess: true`, `decision: APPROVED`
- DB 확인:
  - `loan_application.status` = `APPROVED`
  - `loan_decision` 레코드 생성 (`decision = APPROVED`, `created_by = 지점장 userId`)
  - `notification` 테이블에 **신규 레코드 생성** (`type = LOAN_DECIDED`)

---

### 케이스 4: 지점장(MANAGER)이 거절

**로그인**: `test_manager` 세션 사용

```
POST /api/admin/loan-applications/{케이스4_applicationId}/reject
Content-Type: application/json

{
  "comment": "사업 실적 부진으로 최종 거절"
}
```

**기대 결과**:
- 응답: `isSuccess: true`, `decision: REJECTED`
- DB 확인:
  - `loan_application.status` = `REJECTED`
  - `loan_decision` 레코드 생성 (`decision = REJECTED`, `created_by = 지점장 userId`)
  - `notification` 테이블에 **신규 레코드 생성** (`type = LOAN_DECIDED`)

---

### 에러 케이스 (추가 검증)

| 케이스 | 설명 | 기대 에러 코드 |
|--------|------|---------------|
| 행원이 MANAGER_REVIEW 상태 건 처리 시도 | 권한 없음 | `LOAN_ADMIN4031` |
| 지점장이 SYSTEM_APPROVED 상태 건 처리 시도 | 권한 없음 | `LOAN_ADMIN4031` |
| 이미 APPROVED/REJECTED 된 건 재처리 시도 | 중복 결정 | `LOAN_ADMIN4091` |
| 본인에게 배정되지 않은 건 처리 시도 | 배정 불일치 | `LOAN_ADMIN4032` |

---

## 3. DB 검증 쿼리

테스트 후 결과 확인용:

```sql
-- loan_application 상태 확인
SELECT application_id, status, assigned_banker_id, updated_at
FROM loan_application
ORDER BY application_id DESC LIMIT 4;

-- loan_decision 생성 확인
SELECT decision_id, application_id, decision, approved_amount, approved_rate, comment, created_by, created_at
FROM loan_decision
ORDER BY decision_id DESC LIMIT 4;

-- notification 생성 확인 (지점장 케이스만 존재해야 함)
SELECT notification_id, user_id, type, application_id, is_read, created_at
FROM notification
ORDER BY notification_id DESC LIMIT 4;
```
