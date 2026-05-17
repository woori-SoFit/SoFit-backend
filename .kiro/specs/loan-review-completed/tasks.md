# SOFIT-34 tasks.md
# 심사 완료 대출 목록/상세 조회 API (USER)

- **티켓**: [SOFIT-34] 심사 완료 대출 목록/상세 조회 API
- **브랜치**: `feat/SOFIT-34-심사-완료-대출-목록-상세-조회-API`
- **PR 베이스**: `dev`
- **모듈**: `sofit-user` (+ `sofit-common` 신규 엔티티/Repository 추가)
- **참고 PR**: [#19 SOFIT-32](https://github.com/woori-SoFit/SoFit-backend/pull/19), SOFIT-28 (#15/#18/#20/#21/#22)
- **참고 spec**: `.kiro/specs/loan-review-list/`

---

## 핵심 가이드라인

> SOFIT-28 / SOFIT-32 코드 컨벤션을 그대로 따른다. `loan_decision` 신규 엔티티 추가만 차이점.

- 새 Service/Controller/Repository 클래스 만들지 말 것 — 기존 `LoanService`, `LoanApplicationController`, `LoanApplicationRepository`에 **메서드만 추가**
- `LoanDecision` 엔티티 + Repository만 신규 생성 (`sofit-common`)
- **DTO 스타일**: `public record` 사용 (Response = 불변 = record). 컨벤션 명시 + `auth` 도메인 선례 일치. SOFIT-28/32의 `@Getter @Builder class`는 컨벤션 위반이며 별도 후속 티켓에서 정리 예정 — 본 티켓에서는 따라하지 않음
- **목록 응답**: `List<DTO>` 직접 반환 금지 — wrapper record로 감싸기 (`record CompletedLoanListResponse(List<CompletedLoanItem> loanApplications) { record CompletedLoanItem(...) {} }`)
- **응답 코드**: `LoanSuccessCode`에 신규 코드 2개 추가 (`LOAN2008`, `LOAN2009`)

---

## 커밋 메시지 규칙
- prefix: `[SOFIT-34]`
- 형식: `[SOFIT-34] {Type}: {요약}` (Type: `Feat`, `Refactor`, `Fix`, `Test`, `Docs`, `Chore`)
- **`Co-Authored-By` 라인 절대 추가 금지**

---

## Phase 1. sofit-common — LoanDecision 엔티티/Repository 추가

### Task 1-1. Decision enum
- **파일**: `sofit-common/.../entity/loan/enums/Decision.java`
- 값: `APPROVED`, `REJECTED`

### Task 1-2. LoanDecision 엔티티
- **파일**: `sofit-common/.../entity/loan/LoanDecision.java`
- **테이블**: `loan_decision`
- **컬럼**:
  - `decision_id` (PK, IDENTITY)
  - `application_id` (FK → loan_application, NOT NULL, **unique**)
  - `decision` (ENUM, NOT NULL)
  - `approved_amount` (BIGINT, NULL)
  - `approved_rate` (DECIMAL(5,2), NULL → BigDecimal)
  - `approved_term` (INT, NULL)
  - `rejection_reason` (TEXT, NULL)
- **연관관계**: `@OneToOne(fetch = LAZY)` `@JoinColumn(name = "application_id", unique = true)` → `LoanApplication`
- BaseEntity 상속 여부: 명세에 created_at/updated_at 컬럼 없음 → **상속하지 않음**
- 생성자: `protected LoanDecision()` + 정적 팩토리 메서드는 이번 티켓 범위 외 (조회만 필요)

### Task 1-3. LoanDecisionRepository
- **파일**: `sofit-common/.../repository/LoanDecisionRepository.java`
- **메서드**:
  ```java
  Optional<LoanDecision> findByApplication_ApplicationId(Long applicationId);
  ```

### Task 1-4. LoanApplicationRepository 메서드 추가
- **파일**: `sofit-common/.../repository/LoanApplicationRepository.java`
- **추가**:
  ```java
  List<LoanApplication> findByUser_IdAndStatusInOrderByUpdatedAtDesc(
          Long userId, List<ApplicationStatus> statuses);
  ```
- 단건 조회는 기존 `findByApplicationIdAndUser_Id` 재사용

---

## Phase 2. sofit-user — DTO 추가 (record 스타일)

### Task 2-1. CompletedLoanListResponse (목록 wrapper record)
- **파일**: `sofit-user/.../domain/loan/dto/response/CompletedLoanListResponse.java`
- **스타일**: `public record` — 내부 `public record CompletedLoanItem` 중첩 (wrapper 패턴 유지)
- **외부 필드**:
  - `loanApplications` (`List<CompletedLoanItem>`)
- **내부 record `CompletedLoanItem` 필드**:
  - `applicationId` (Long)
  - `productName` (String)
  - `status` (ApplicationStatus)
  - `requestedAmount` (Long)
  - `appliedAt` (LocalDate)
  - `updatedAt` (LocalDate)
- 구조 예시:
  ```java
  public record CompletedLoanListResponse(
          List<CompletedLoanItem> loanApplications
  ) {
      public record CompletedLoanItem(
              Long applicationId,
              String productName,
              ApplicationStatus status,
              Long requestedAmount,
              LocalDate appliedAt,
              LocalDate updatedAt
      ) {}
  }
  ```

### Task 2-2. CompletedLoanDetailResponse (상세 record)
- **파일**: `sofit-user/.../domain/loan/dto/response/CompletedLoanDetailResponse.java`
- **스타일**: `public record`
- **필드**:
  - `applicationId` (Long)
  - `productName` (String)
  - `requestedAmount` (Long)
  - `repaymentMethod` (RepaymentMethod)
  - `decisionInfo` (DecisionInfo) — 내부 record

### Task 2-3. CompletedLoanDetailResponse.DecisionInfo
- 내부 `public record`
- **필드**:
  - `decision` (Decision)
  - `approvedAmount` (Long, nullable)
  - `approvedRate` (BigDecimal, nullable)
  - `approvedTerm` (Integer, nullable)
  - `rejectionReason` (String, nullable)
- `@JsonInclude(JsonInclude.Include.ALWAYS)` 어노테이션 record에 부착 — null 필드도 응답에 포함 (스펙 명시)

---

## Phase 3. Converter 메서드 추가

### Task 3-1. LoanConverter 메서드 추가
- **파일**: `sofit-user/.../domain/loan/converter/LoanConverter.java`
- **추가 메서드**:
  - `toCompletedListItem(LoanApplication)` → `CompletedLoanListResponse.CompletedLoanItem`
    - `appliedAt`, `updatedAt`: `LocalDateTime → LocalDate` 변환 (null 가드)
    - record 생성자 직접 호출 (`new CompletedLoanItem(...)`)
  - `toCompletedDetailResponse(LoanApplication, LoanDecision)` → `CompletedLoanDetailResponse`
    - `DecisionInfo` record를 `new`로 조립 후 외부 record 생성자에 주입

---

## Phase 4. Service 메서드 추가

### Task 4-1. LoanErrorCode 추가
- **파일**: `sofit-user/.../domain/loan/exception/LoanErrorCode.java`
- **추가**: `LOAN_DECISION_NOT_FOUND` (404, **`LOAN4043`**, "심사 결정 정보를 찾을 수 없습니다.")
  - ⚠️ `LOAN4041`은 `PRODUCT_NOT_FOUND`, `LOAN4042`는 `APPLICATION_NOT_FOUND`에 이미 사용 중 → `LOAN4043` 부여

### Task 4-2. LoanSuccessCode 추가
- **파일**: `sofit-user/.../domain/loan/exception/LoanSuccessCode.java`
- **추가**:
  - `LOAN_APPLICATION_COMPLETED_LIST_OK` (200, `LOAN2008`, "심사 완료 대출 목록 조회에 성공했습니다.")
  - `LOAN_APPLICATION_COMPLETED_DETAIL_OK` (200, `LOAN2009`, "심사 완료 대출 상세 조회에 성공했습니다.")

### Task 4-3. LoanService 인터페이스
- **파일**: `sofit-user/.../domain/loan/service/LoanService.java`
- **추가**:
  ```java
  CompletedLoanListResponse findCompletedLoans(Long userId);
  CompletedLoanDetailResponse findCompletedLoanDetail(Long userId, Long applicationId);
  ```

### Task 4-4. LoanServiceImpl 구현
- **파일**: `sofit-user/.../domain/loan/service/LoanServiceImpl.java`
- `LoanDecisionRepository` 의존성 추가
- **상수**:
  ```java
  private static final List<ApplicationStatus> COMPLETED_STATUSES = List.of(
          ApplicationStatus.APPROVED,
          ApplicationStatus.REJECTED
  );
  ```
- **findCompletedLoans**:
  - `findByUser_IdAndStatusInOrderByUpdatedAtDesc(userId, COMPLETED_STATUSES)`
  - `LoanConverter::toCompletedListItem` 매핑 → `List<CompletedLoanItem>` 빌드
  - `new CompletedLoanListResponse(items)` 반환
- **findCompletedLoanDetail**:
  1. `findByApplicationIdAndUser_Id(applicationId, userId)` → 없으면 `LOAN4042`(APPLICATION_NOT_FOUND)
  2. status가 `COMPLETED_STATUSES`에 없으면 `LOAN4042`(심사중 건 차단)
  3. `LoanDecisionRepository.findByApplication_ApplicationId(applicationId)` → 없으면 `LOAN4043`(LOAN_DECISION_NOT_FOUND)
  4. `LoanConverter::toCompletedDetailResponse(application, decision)` 반환

---

## Phase 5. Controller 엔드포인트 추가

### Task 5-1. LoanApplicationController 엔드포인트 추가
- **파일**: `sofit-user/.../domain/loan/controller/LoanApplicationController.java`
- **추가** (SOFIT-32 패턴 미러링 — 응답 변수명 `response`, `LoanSuccessCode` 사용):
  ```java
  @GetMapping("/completed")
  public ApiResponse<CompletedLoanListResponse> getCompletedLoans() {
      CompletedLoanListResponse response = loanService.findCompletedLoans(TEMP_USER_ID);
      return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_COMPLETED_LIST_OK, response);
  }

  @GetMapping("/completed/{applicationId}")
  public ApiResponse<CompletedLoanDetailResponse> getCompletedLoanDetail(
          @PathVariable Long applicationId) {
      CompletedLoanDetailResponse response = loanService.findCompletedLoanDetail(TEMP_USER_ID, applicationId);
      return ApiResponse.onSuccess(LoanSuccessCode.LOAN_APPLICATION_COMPLETED_DETAIL_OK, response);
  }
  ```
- ⚠️ `GeneralSuccessCode.OK` / `COMMON2000` 직접 사용 금지 — 컨벤션상 도메인 SuccessCode 사용

---

## Phase 6. 검증

### Task 6-1. 빌드
```bash
./gradlew :sofit-common:build -x test
./gradlew :sofit-user:build -x test
```

### Task 6-2. 테스트 데이터 준비
- `loan_application` 1건 (status: APPROVED), `loan_decision` 1건 (decision: APPROVED, approved_amount/rate/term 채움)
- `loan_application` 1건 (status: REJECTED), `loan_decision` 1건 (decision: REJECTED, rejection_reason 채움)

### Task 6-3. Apidog 수동 테스트
- `GET /api/loan-applications/completed` → `result.loanApplications` 배열, updatedAt DESC 정렬 확인, code `LOAN2008`
- `GET /api/loan-applications/completed/{승인ID}` → decisionInfo 승인 필드 채워짐, rejectionReason null, code `LOAN2009`
- `GET /api/loan-applications/completed/{거절ID}` → decisionInfo rejectionReason 채워짐, approved* null
- `GET /api/loan-applications/completed/{심사중ID}` → 404 `LOAN4042`
- `GET /api/loan-applications/completed/{타사용자ID}` → 404 `LOAN4042`
- `GET /api/loan-applications/completed/{decision없는ID}` → 404 `LOAN4043`

---

## 커밋 분리 가이드

```
[SOFIT-34] Feat: LoanDecision 엔티티 및 Repository 추가
[SOFIT-34] Feat: 심사 완료 대출 목록/상세 조회 API 구현
[SOFIT-34] Docs: 심사 완료 대출 조회 API spec 문서 추가
```

---

## 체크리스트

- [ ] `LoanDecision` 엔티티가 `sofit-common`에 위치하는가?
- [ ] `application_id`에 unique 제약이 있는가?
- [ ] DTO가 `record` 스타일인가? (Response = 불변 = record, 컨벤션 준수)
- [ ] 목록 응답이 wrapper record(`CompletedLoanListResponse(List<CompletedLoanItem>)`)로 감싸졌는가?
- [ ] 상세 응답에 `status`, `appliedAt`, `updatedAt` 필드가 **없는가**?
- [ ] DecisionInfo의 null 필드가 응답에 포함되는가? (`@JsonInclude.ALWAYS`)
- [ ] 상세 조회 시 status가 APPROVED/REJECTED 아니면 404 `LOAN4042`?
- [ ] decision 레코드 없으면 404 `LOAN4043`?
- [ ] 목록이 `updatedAt DESC`로 정렬되는가?
- [ ] 응답 코드가 `LoanSuccessCode.LOAN_APPLICATION_COMPLETED_LIST_OK / _DETAIL_OK`인가?
- [ ] Controller 응답 변수명이 `response`인가?
- [ ] Entity ↔ DTO 변환이 모두 `LoanConverter`에서 처리되는가?
- [ ] 모든 커밋이 `[SOFIT-34]` prefix?
- [ ] `Co-Authored-By` 없는가?

---

## TODO (후속)
- 세션 인증 적용 (`TEMP_USER_ID` 제거)
- 페이지네이션 (`Pageable`)
- LoanDecision 생성 로직 (심사 처리 API에서) — 별도 티켓
