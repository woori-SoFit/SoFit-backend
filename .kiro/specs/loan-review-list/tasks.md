# SOFIT-32 tasks.md
# 심사 중인 대출 목록/상세 조회 API (USER)

- **브랜치**: `feat/SOFIT-32-심사중인-대출-목록/상세-조회-API`
- **PR 베이스**: `dev`
- **모듈**: `sofit-user` (USER가 본인 대출 조회)
- **백업**: `backup/SOFIT-32-admin-loan-review-list` (BANK_ADMIN용 작업 보존)

---

## 최종 커밋 구성

```
62e8c7a  [SOFIT-32] Fix: loan enums 패키지 선언 오류 수정 및 LoanApplication 엔티티 정비
27882ca  [SOFIT-32] Feat: 사용자 본인 심사 중인 대출 목록/상세 조회 API 구현
24c2526  [SOFIT-32] Chore: sofit-user 의존성 및 설정 추가, .kiro/specs gitignore 처리
```

---

## Phase 1. sofit-common 정비 ✅

### Task 1-1. loan enums 패키지 선언 오류 수정
- **대상 파일** (모두 `entity.loan` → `entity.loan.enums`로 수정):
  - `ApplicationStatus.java`
  - `RepaymentMethod.java`
  - `AnnualIncome.java`
  - `CreditScoreRange.java`
  - `IncomeType.java`
  - `LoanPurpose.java`
  - `ExistingLoanAmount.java`
  - `ProductStatus.java`

### Task 1-2. LoanApplication 엔티티 정비
- **파일**: `sofit-common/.../entity/loan/LoanApplication.java`
- **수정 내용**:
  - enum import 경로 수정 (`loan.enums.*`)
  - `s_evaluation_id` 컬럼명 유지 (팀원 원본 기준)
  - `userInputExistingLoanAmt` 타입 `ExistingLoanAmount` Enum 유지

### Task 1-3. LoanProductRepository import 수정
- **파일**: `sofit-common/.../repository/LoanProductRepository.java`
- `ProductStatus` import 경로 수정

### Task 1-4. mysql-connector 버전 고정
- **파일**: `sofit-common/build.gradle`
- `mysql-connector-j:8.3.0` 고정 (Java 21 호환)

---

## Phase 2. sofit-user API 구현 ✅

### Task 2-1. LoanApplicationRepository (user 전용)
- **파일**: `sofit-user/.../domain/loan/repository/LoanApplicationRepository.java`
- **메서드**:
  - `findByUser_IdAndStatusIn(Long userId, List<ApplicationStatus> statuses)`
  - `findByApplicationIdAndUser_Id(Long applicationId, Long userId)`

### Task 2-2. LoanErrorCode
- **파일**: `sofit-user/.../domain/loan/exception/LoanErrorCode.java`
- `LOAN_APPLICATION_NOT_FOUND` (404, LOAN4041)

### Task 2-3. DTO
- `LoanApplicationListResponse` — `applicationId`, `productName`, `status`, `requestedAmount`, `appliedAt(LocalDate)`
- `LoanApplicationDetailResponse` — 위 + `requestedTerm`, `repaymentMethod`, `appliedAt(LocalDateTime)`

### Task 2-4. LoanConverter
- `toListResponse(LoanApplication)` → `LoanApplicationListResponse`
- `toDetailResponse(LoanApplication)` → `LoanApplicationDetailResponse`

### Task 2-5. LoanService / LoanServiceImpl
- `findUnderReviewLoans(Long userId)` — 심사 중 상태 5개 필터링
- `findLoanDetail(Long userId, Long applicationId)` — 본인 소유 검증 포함 (없으면 LOAN4041)

### Task 2-6. LoanController
- `GET /api/loan-applications` → `findUnderReviewLoans(TEMP_USER_ID)`
- `GET /api/loan-applications/{applicationId}` → `findLoanDetail(TEMP_USER_ID, applicationId)`
- `TEMP_USER_ID = 1L` 하드코딩 (TODO: 세션 인증 후 교체)

---

## Phase 3. sofit-user 설정 ✅

### Task 3-1. SofitUserApplication
- `@EntityScan("com.sofit.common.entity")` — common 엔티티 스캔
- `@EnableJpaRepositories` — common + user Repository 스캔
- `@SpringBootApplication(scanBasePackages = "com.sofit")` — common 컴포넌트 스캔

### Task 3-2. SecurityConfig (임시)
- **파일**: `sofit-user/.../global/config/SecurityConfig.java`
- 모든 요청 `permitAll()` (TODO: 세션 인증 구현 후 USER 권한 체크로 교체)

### Task 3-3. build.gradle
- JPA 의존성 추가
- Security 의존성 추가
- `.env` 파일 bootRun 환경변수 주입 설정

### Task 3-4. application.yml
- `ddl-auto: none` (validate → none)
- 불필요한 주석 정리

---

## TODO (후속 작업)

| 항목 | 내용 |
|------|------|
| 세션 인증 | `TEMP_USER_ID` 제거 → `SecurityContext`에서 userId 추출 |
| SecurityConfig | `permitAll()` → USER 권한 체크로 교체 |
| BANK_ADMIN API | `backup/SOFIT-32-admin-loan-review-list` 브랜치 활용, 별도 티켓 |
