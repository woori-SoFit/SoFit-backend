# 대출 신청 플로우 개발 방향 정리



## 1. 대출 신청 플로우 API 설계

각 단계 완료 시마다 DB에 즉시 저장하며, `applicationId` 기반으로 단계를 이어간다.

### Step 1. 1차 필터링 (신청 가능 여부 확인)

```
POST /api/loan-products/{productId}/eligibility-check
```

**요청값**
```json
{
  "annualIncome": "AMT_0_30M",
  "creditScore": "CS_850_OVER",
  "incomeType": "02",
  "existingLoanAmt": "LOAN_0_100M"
}
```

**처리 로직**
- 세션에서 `userId` 추출 (인증 확인 — 로그인 세션 구현 완료 상태)
- 상품 조건과 요청값을 비교하여 eligible 판정
  1. 연소득: 고객 소득 범위가 `annual_income_limit` 이상이면 통과
  2. 신용점수: 고객 신용점수 범위가 `credit_score_limit` 이상이면 통과
  3. 소득 종류: `income_type_code_limit`이 NULL이거나 일치하면 통과
  4. 기보유 대출액: 고객 대출액이 `existing_loan_amt_limit` 이하이면 통과
- **eligible 여부와 무관하게** `loan_application` 레코드를 즉시 DB에 생성
  - **eligible = true** → status = `DRAFT`, last_completed_step = `ELIGIBILITY_DONE`
  - **eligible = false** → status = `REJECTED`, last_completed_step = `ELIGIBILITY_CHECKING`
- 응답으로 `applicationId` 반환 → eligible=true인 경우 이후 모든 단계는 이 ID 기반으로 진행

**응답값 (eligible = true)**
```json
{
  "eligible": true,
  "applicationId": 10293
}
```

**응답값 (eligible = false)**
```json
{
  "eligible": false,
  "applicationId": 10294
}
```

### Step 2 이후 (약관 동의 / 본인인증 / 사업자 확인 / 마이데이터 수집)

```
POST /api/loans/applications/{applicationId}/consents      // 약관 동의
POST /api/loans/applications/{applicationId}/auth          // 본인인증
GET  /api/loans/applications/{applicationId}/biz-info      // 사업자 정보 확인
POST /api/loans/applications/{applicationId}/collect-data  // 마이데이터 수집
```

- 각 단계 완료 시마다 해당 `applicationId` 레코드 업데이트
- `last_completed_step` 컬럼으로 현재까지 완료된 단계 추적

### Step 7. 최종 제출 (심사 요청)

```
POST /api/loans/applications/{applicationId}/submit
```

**요청값**
```json
{
  "requestedAmount": 100000000,
  "term": 60,
  "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST",
  "purpose": "WORKING_CAPITAL"
}
```

- status: `DRAFT` → `SUBMITTED` 변경
- `applied_at` 이 시점에 기록

**응답값 (200, code: LOAN2005)**
```json
{
  "isSuccess": true,
  "code": "LOAN2005",
  "message": "대출 심사 요청에 성공했습니다.",
  "result": {
    "applicationId": 1,
    "productName": "카카오뱅크 개인사업자 신용대출",
    "requestedAmount": 100000000,
    "applied_at": "2024-05-12T14:35:00",
    "repaymentMethod": "EQUAL_PRINCIPAL_INTEREST",
    "notification_enabled": true
  }
}

---

## 3. DRAFT 상태 설계

### DRAFT란?
- 사용자가 1차 필터링을 통과한 순간부터 최종 제출 전까지의 상태
- "아직 제출되지 않은 신청서(초안)"를 의미
- 각 단계를 완료할 때마다 DB에 즉시 저장되므로, 중간에 앱을 종료해도 데이터는 보존됨

### loan_application 테이블 추가 컬럼

| 컬럼명 | 타입 | 설명 |
|---|---|---|
| `last_completed_step` | VARCHAR(30) | 마지막으로 완료한 단계 |
| `draft_expired_at` | DATETIME | DRAFT 만료 일시 |

### last_completed_step 값 정의

| 값 | 설명 |
|---|---|
| `ELIGIBILITY_CHECKING` | 1차 필터링 진행 (탈락 시 이 단계에서 REJECTED) |
| `ELIGIBILITY_DONE` | 1차 필터링 통과 |
| `CONSENT_DONE` | 약관 동의 완료 |
| `AUTH_DONE` | 본인인증 완료 |
| `BIZ_INFO_DONE` | 사업자 정보 확인 완료 |
| `DATA_COLLECTED` | 마이데이터 수집 완료 |

### DRAFT 만료 시간
- 세션 만료 시간과 동일하게 설정
- 세션이 먼저 만료되면 DRAFT가 남아도 재로그인이 필요하므로, 만료 시간을 맞추는 것이 자연스러움
- 만료된 DRAFT는 스케줄러로 주기적으로 정리

```java
@Scheduled(cron = "0 0 2 * * *") // 매일 새벽 2시
public void expireDraftApplications() {
    loanApplicationRepository
        .updateStatusToExpiredWhereDraftExpiredAtBefore(LocalDateTime.now());
}
```

---

## 4. 이어가기 플로우

> DRAFT 만료(1시간) 이후에는 새로 신청하도록 안내.

### 앱 진입 시 DRAFT 감지

```
GET /api/loans/applications/draft?productId={productId}
```

**응답값**
```json
{
  "hasDraft": true,
  "applicationId": 10293,
  "lastCompletedStep": "CONSENT_DONE",
  "resumeStep": "AUTH",
  "expiredAt": "2024-05-12T15:30:00"
}
```

### 이어가기 플로우

```
[앱 실행 or 상품 상세 진입]
         ↓
GET /api/loans/applications/draft?productId={productId}
         ↓
    hasDraft: true?
    ↙              ↘
이어가기            새로 시작
모달 표시           1차 필터링부터
    ↓
사용자가 "이어가기" 선택
    ↓
resumeStep 화면으로 이동
(applicationId 들고 해당 step API 호출)
```

### 이어가기 데이터 복원

```
GET /api/loans/applications/{applicationId}/resume
```

**응답값**: 지금까지 저장된 데이터 전체 반환 (미입력 필드는 null)

```json
{
  "applicationId": 10293,
  "resumeStep": "AUTH",
  "savedData": {
    "annualIncome": "AMT_0_30M",
    "creditScore": "CS_850_OVER",
    "incomeType": "02",
    "existingLoanAmt": "LOAN_0_100M",
    "consentsAgreed": true,
    "requestedAmount": null
  }
}
```

---

## 5. 전체 데이터 흐름 요약

| 저장 위치 | 저장 대상 |
|---|---|
| **세션 (Redis)** | userId, roles 등 인증 정보만 |
| **DB (loan_application)** | 고객 입력값, 단계별 진행 상태 등 비즈니스 데이터 |
| **프론트 (메모리)** | applicationId 등 다음 단계로 넘길 식별자 |
