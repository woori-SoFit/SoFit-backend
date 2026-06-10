# 대출 실행 완료 목록 조회 API

## 개요
- EXECUTED 상태의 대출 실행 완료 건을 목록으로 조회하는 API
- 엔드포인트: `GET /api/loan-executions`
- 기존 `/api/loan-applications/{applicationId}/execution` 응답을 참고하되, 실행일(executedAt) 필드 추가

## 브랜치/커밋 정보
- 브랜치: `feat/SOFIT-XXX-loan-execution-list`
- 커밋: `[SOFIT-XXX] Feat: 대출 실행 완료 목록 조회 API 구현`

---

## Phase 1: Response DTO 생성

### 파일 생성
- `sofit-user/.../loan/dto/response/LoanExecutionListResponse.java`
  - 내부에 List<LoanExecutionItem>을 감싸는 래퍼 DTO
- `sofit-user/.../loan/dto/response/LoanExecutionItemResponse.java`
  - 기존 LoanExecutionResultResponse 필드 + `executedAt` (LocalDateTime) 추가

### 응답 형태
```json
{
  "isSuccess": true,
  "code": "LOAN2019",
  "message": "대출 실행 완료 목록 조회에 성공했습니다.",
  "result": {
    "executions": [
      {
        "executionId": 1,
        "applicationId": 10,
        "productId": 1,
        "productName": "소상공인 성장 대출",
        "executedAmount": 50000000,
        "approvedRate": 4.50,
        "approvedTerm": 36,
        "repaymentMethod": "EQUAL_PRINCIPAL_AND_INTEREST",
        "executedAt": "2025-06-10T15:30:00"
      }
    ]
  }
}
```

---

## Phase 2: Repository 메서드 추가

### 파일 수정
- `sofit-common/.../repository/loan/LoanExecutionRepository.java`
  - `findAllByUserId(Long userId)` 메서드 추가 (EXECUTED 상태 + user 필터 + fetch join)

---

## Phase 3: Service 메서드 추가

### 파일 수정
- `sofit-user/.../loan/service/LoanExecutionService.java` (인터페이스)
  - `findExecutionList(Long userId)` 메서드 추가
- `sofit-user/.../loan/service/LoanExecutionServiceImpl.java`
  - 구현체 메서드 추가 (실행 목록 조회 + 각 건별 Decision 조회)

---

## Phase 4: Converter 메서드 추가

### 파일 수정
- `sofit-user/.../loan/converter/LoanExecutionConverter.java`
  - `toItemResponse()` 메서드 추가
  - `toListResponse()` 메서드 추가

---

## Phase 5: Controller + Docs + SuccessCode

### 파일 수정
- `sofit-user/.../loan/exception/LoanSuccessCode.java`
  - `LOAN_EXECUTION_LIST_OK` 추가
- `sofit-user/.../loan/controller/LoanExecutionControllerDocs.java`
  - 목록 조회 Swagger 어노테이션 추가
- `sofit-user/.../loan/controller/LoanExecutionController.java`
  - `GET /api/loan-executions` 엔드포인트 추가

---

## 확인사항
- 실행일(executedAt) = LoanExecution의 createdAt (BaseEntity)
- N+1 방지: fetch join으로 application, product 함께 조회
- LoanDecision도 각 건별로 조회 필요 (MANAGER_APPROVED 상태)
