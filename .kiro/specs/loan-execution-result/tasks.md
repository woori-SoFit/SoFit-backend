# SOFIT-40 tasks.md
# 대출 실행 결과 조회 API (USER)

- **브랜치**: `feat/SOFIT-40-대출-실행-결과-조회-API` (생성 완료)
- **PR 베이스**: `dev`
- **모듈**: `sofit-user` (USER가 본인 대출 실행 결과 조회)

---

## Phase 1. sofit-common 엔티티 추가

### Task 1-1. LoanExecution 엔티티 생성
- **파일**: `sofit-common/src/main/java/com/sofit/common/entity/loan/LoanExecution.java`
- **내용**:
  - `execution_id` (PK, BIGINT, AUTO_INCREMENT)
  - `application_id` (FK → loan_application, UNIQUE, @OneToOne LAZY)
  - `execution_amount` (Long, NOT NULL)
  - `account_number` (String)
  - `bank_code` (String)
  - `@NoArgsConstructor(access = PROTECTED)`, `@Getter`

### Task 1-2. LoanDecision 엔티티 생성
- **파일**: `sofit-common/src/main/java/com/sofit/common/entity/loan/LoanDecision.java`
- **내용**:
  - `decision_id` (PK, BIGINT, AUTO_INCREMENT)
  - `application_id` (FK → loan_application, @ManyToOne LAZY)
  - `decision` (ENUM: APPROVED, REJECTED)
  - `approved_amount` (Long)
  - `approved_rate` (BigDecimal)
  - `approved_term` (Integer)
  - `rejection_reason` (String)
  - 내부 enum `Decision { APPROVED, REJECTED }`

### Task 1-3. 커밋
```
[SOFIT-40] Feat: LoanExecution, LoanDecision 엔티티 추가
```

---

## Phase 2. sofit-user 조회 API 구현

### Task 2-1. LoanExecutionRepository 생성
- **파일**: `sofit-common/src/main/java/com/sofit/common/repository/LoanExecutionRepository.java` (JpaConfig basePackages 제한으로 common 모듈에 위치)
- **메서드**: `findByApplicationIdAndUserId(Long applicationId, Long userId)`
  - JPQL fetch join: `e.application a`, `a.product`
  - 조건: `a.applicationId = :applicationId AND a.user.id = :userId AND a.status = 'EXECUTED'`

### Task 2-2. LoanDecisionRepository — SOFIT-34에서 추가됨, 그대로 재사용
- **파일**: `sofit-common/src/main/java/com/sofit/common/repository/LoanDecisionRepository.java`
- **메서드**: `findByApplication_ApplicationId(Long applicationId)` (dev 머지 후 SOFIT-34 정의 채택)

### Task 2-3. LoanErrorCode에 EXECUTION_NOT_FOUND 추가
- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/loan/exception/LoanErrorCode.java`
- **추가**: `EXECUTION_NOT_FOUND(HttpStatus.NOT_FOUND, "LOAN4044", "실행 건을 찾을 수 없습니다.")`

### Task 2-4. LoanSuccessCode에 LOAN_EXECUTION_RESULT_OK 추가
- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/loan/exception/LoanSuccessCode.java`
- **추가**: `LOAN_EXECUTION_RESULT_OK(HttpStatus.OK, "LOAN2010", "대출 실행 결과 조회에 성공했습니다.")`

### Task 2-5. LoanExecutionResultResponse DTO 생성
- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/loan/dto/response/LoanExecutionResultResponse.java`
- **타입**: record (Response DTO 규칙)
- **필드**: executionId, applicationId, productId, productName, executedAmount, approvedRate, approvedTerm, repaymentMethod

### Task 2-6. LoanExecutionConverter 생성
- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/loan/converter/LoanExecutionConverter.java`
- **메서드**: `toResponse(LoanExecution execution, LoanDecision decision)` → `LoanExecutionResultResponse`

### Task 2-7. LoanExecutionService 인터페이스 생성
- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/loan/service/LoanExecutionService.java`
- **메서드**: `LoanExecutionResultResponse findExecutionResult(Long userId, Long applicationId)`

### Task 2-8. LoanExecutionServiceImpl 구현
- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/loan/service/LoanExecutionServiceImpl.java`
- **로직**:
  1. `loanExecutionRepository.findByApplicationIdAndUserId(applicationId, userId)` → 없으면 EXECUTION_NOT_FOUND (LOAN4044)
  2. `loanDecisionRepository.findByApplication_ApplicationId(applicationId)` → 없으면 LOAN_DECISION_NOT_FOUND (LOAN4043, SOFIT-34 정의 재사용)
  3. `LoanExecutionConverter.toResponse(execution, decision)` 반환
- `@Transactional(readOnly = true)`

### Task 2-9. LoanExecutionControllerDocs 인터페이스 생성
- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/loan/controller/LoanExecutionControllerDocs.java`
- Swagger `@Operation`, `@ApiResponses` 어노테이션 정의

### Task 2-10. LoanExecutionController 생성
- **파일**: `sofit-user/src/main/java/com/sofit/user/domain/loan/controller/LoanExecutionController.java`
- `@RestController`, `@RequestMapping("/api/loan-applications")`
- `GET /{applicationId}/execution` → `findExecutionResult(TEMP_USER_ID, applicationId)`
- `TEMP_USER_ID = 1L` 하드코딩 (TODO: 세션 인증 후 교체)
- `implements LoanExecutionControllerDocs`

### Task 2-11. 커밋
```
[SOFIT-40] Feat: 대출 실행 결과 조회 API 구현
```

---

## Phase 3. 컴파일 확인 및 정리

### Task 3-1. 컴파일 확인
- `./gradlew :sofit-user:compileJava` 실행하여 오류 없음 확인

### Task 3-2. (필요 시) import/설정 수정
- EntityScan에 새 엔티티 포함 여부 확인
- 기존 `@EntityScan("com.sofit.common.entity")` 범위에 포함되므로 추가 설정 불필요 예상

---

## TODO (후속 작업)

| 항목 | 내용 |
|------|------|
| 세션 인증 | `TEMP_USER_ID` 제거 → `SecurityContext`에서 userId 추출 |
| SecurityConfig | `permitAll()` → USER 권한 체크로 교체 |
