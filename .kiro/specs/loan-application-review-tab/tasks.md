# Implementation Plan: 대출 신청 심사 결과 탭 API

## Overview

기존 `LoanDashboardController`에 `GET /{applicationId}/review` 엔드포인트를 추가하여 심사 결과 탭 데이터를 조회하는 API를 구현합니다. LoanDecision 엔티티 수정 → Repository 확장 → DTO 생성 → Converter 생성 → Service 생성 → Controller 확장 → SuccessCode 추가 → Swagger 문서 추가 순서로 진행합니다.

## Tasks

- [x] 1. LoanDecision 엔티티 수정 및 Repository 확장
  - [x] 1.1 LoanDecision 엔티티에 createdAt, createdBy 필드 추가
    - `sofit-common/src/main/java/com/sofit/common/entity/loan/LoanDecision.java` 수정
    - `createdAt` (LocalDateTime, `@Column(name = "created_at", updatable = false)`) 필드 추가
    - `createdBy` (Long, nullable, `@Column(name = "created_by", updatable = false)`) 필드 추가
    - 기존 `@OneToOne` 관계를 `@ManyToOne`으로 변경 (동일 application에 여러 decision 존재 가능)
    - `unique = true` 제약 조건 제거
    - _Requirements: 8.1, 8.2_

  - [x] 1.2 LoanDecisionRepository에 findAll 메서드 추가
    - `sofit-common/src/main/java/com/sofit/common/repository/LoanDecisionRepository.java` 수정
    - `List<LoanDecision> findAllByApplication_ApplicationIdOrderByCreatedAtAsc(Long applicationId)` 메서드 추가
    - _Requirements: 7.1, 7.2_

- [x] 2. Response DTO 생성
  - [x] 2.1 LoanApplicationReviewResponse record 및 중첩 record 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/dto/response/LoanApplicationReviewResponse.java` 생성
    - 최상위 record: `productInfo`, `applicationInfo`, `recommendation`(nullable), `decisions`
    - 중첩 record: `ProductInfoResponse`, `ApplicationInfoResponse`, `RecommendationResponse`, `DecisionResponse`
    - ProductInfoResponse: productName, minAmount, maxAmount, minInterestRate, maxInterestRate, minTermMonths, maxTermMonths, availableRepaymentMethods(List<String>), availablePurposes(List<String>)
    - ApplicationInfoResponse: requestedAmount, requestedTerm, purpose, repaymentMethod
    - RecommendationResponse: approvedAmount, approvedRate, approvedTerm, repaymentMethod
    - DecisionResponse: status, comment, reviewerName, reviewerRole, decidedAt(LocalDateTime)
    - _Requirements: 9.2, 9.3_

- [x] 3. Converter 생성
  - [x] 3.1 LoanApplicationReviewConverter 클래스 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/converter/LoanApplicationReviewConverter.java` 생성
    - private 생성자 선언 (인스턴스화 방지)
    - `toProductInfoResponse(LoanProduct, List<LoanProductOption>)` 정적 메서드 구현
      - LoanProductOption에서 repaymentMethod, purpose를 중복 제거하여 리스트로 변환
    - `toApplicationInfoResponse(LoanApplication)` 정적 메서드 구현
      - null 필드는 null로 유지
    - `toRecommendationResponse(LoanDecision)` 정적 메서드 구현
      - created_by == null && decision == APPROVED인 경우만 값 반환, 아니면 null
    - `toDecisionResponse(LoanDecision, User)` 정적 메서드 구현
      - created_by == null: reviewerName="시스템", reviewerRole="SYSTEM", APPROVED면 status="SYSTEM_APPROVED"
      - created_by != null && User 존재: reviewerName=user.name, reviewerRole=user.role.name()
      - created_by != null && User 미존재: reviewerName="알 수 없음", reviewerRole="SYSTEM"
    - `toLoanApplicationReviewResponse(...)` 정적 메서드 구현
    - _Requirements: 9.1, 9.4, 9.5, 4.1, 4.2, 4.3, 5.1, 5.2, 6.1, 7.3, 7.4, 7.5, 7.6, 7.7_

  - [ ]* 3.2 Property test: ProductInfo 변환 정확성
    - **Property 1: ProductInfo 변환 정확성**
    - jqwik을 사용하여 임의의 LoanProduct + LoanProductOption 목록 생성
    - toProductInfoResponse 결과가 원본 필드값을 정확히 반영하는지 검증
    - availableRepaymentMethods, availablePurposes가 중복 제거된 집합과 일치하는지 검증
    - **Validates: Requirements 4.1, 4.2, 4.3**

  - [ ]* 3.3 Property test: ApplicationInfo 변환 정확성
    - **Property 2: ApplicationInfo 변환 정확성**
    - jqwik을 사용하여 임의의 LoanApplication(nullable 필드 포함) 생성
    - toApplicationInfoResponse 결과가 원본 값을 정확히 반영하는지 검증
    - null 필드가 null로 유지되는지 검증
    - **Validates: Requirements 5.1, 5.2**

  - [ ]* 3.4 Property test: Recommendation 변환 정확성
    - **Property 3: Recommendation 변환 정확성**
    - jqwik을 사용하여 created_by == null && decision == APPROVED인 LoanDecision 생성
    - toRecommendationResponse 결과가 approvedAmount, approvedRate, approvedTerm을 정확히 반영하는지 검증
    - **Validates: Requirements 6.1**

  - [ ]* 3.5 Property test: 시스템 심사 DecisionResponse 변환 정확성
    - **Property 4: 시스템 심사(created_by == null) → DecisionResponse 변환 정확성**
    - jqwik을 사용하여 created_by == null인 LoanDecision 생성
    - reviewerName == "시스템", reviewerRole == "SYSTEM" 검증
    - decision == APPROVED일 때 status == "SYSTEM_APPROVED" 검증
    - **Validates: Requirements 7.3, 7.5**

  - [ ]* 3.6 Property test: 은행원 심사 DecisionResponse 변환 정확성
    - **Property 5: 은행원 심사(created_by != null) → DecisionResponse 변환 정확성**
    - jqwik을 사용하여 created_by != null인 LoanDecision + User 생성
    - reviewerName == user.name, reviewerRole == user.role.name() 검증
    - status == decision.name() 검증
    - **Validates: Requirements 7.4, 7.6**

  - [ ]* 3.7 Property test: Decision 시간순 정렬
    - **Property 6: Decision 시간순 정렬**
    - jqwik을 사용하여 임의의 LoanDecision 목록 생성
    - 변환된 decisions 배열 크기가 원본과 동일한지 검증
    - decidedAt 기준 오름차순(비감소) 정렬 검증
    - **Validates: Requirements 7.1, 7.2**

- [x] 4. Checkpoint - 엔티티, DTO, Converter 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Service 레이어 구현
  - [x] 5.1 LoanApplicationReviewService 인터페이스 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/LoanApplicationReviewService.java` 생성
    - `LoanApplicationReviewResponse findLoanApplicationReview(Long applicationId)` 메서드 선언
    - _Requirements: 1.1_

  - [x] 5.2 LoanApplicationReviewServiceImpl 구현체 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/LoanApplicationReviewServiceImpl.java` 생성
    - `@Service @RequiredArgsConstructor @Transactional(readOnly = true)` 어노테이션
    - 의존성: LoanApplicationRepository, LoanProductOptionRepository, LoanDecisionRepository, UserRepository
    - 로직 흐름:
      1. LoanApplication 조회 (없으면 BaseException(GeneralErrorCode.NOT_FOUND))
      2. LoanProduct 추출 (application.getProduct())
      3. LoanProductOption 목록 조회 (by productId)
      4. LoanDecision 전체 목록 조회 (findAllByApplication_ApplicationIdOrderByCreatedAtAsc)
      5. 시스템 심사 추출: created_by == null && decision == APPROVED인 건 → Recommendation
      6. 은행원 심사의 createdBy로 User 조회
      7. Converter로 DTO 변환 후 반환
    - _Requirements: 1.1, 3.1, 4.1, 5.1, 6.1, 6.2, 6.3, 7.1, 7.8_

  - [ ]* 5.3 LoanApplicationReviewServiceImpl 단위 테스트 작성
    - JUnit 5 + Mockito 사용
    - applicationId 미존재 시 NOT_FOUND 예외 발생 테스트
    - 시스템 심사 APPROVED 시 recommendation 정상 반환 테스트
    - 시스템 심사 REJECTED 시 recommendation = null 테스트
    - 심사 이력 미존재 시 빈 배열 반환 테스트
    - createdBy 사용자 미존재 시 폴백 값 적용 테스트
    - _Requirements: 3.1, 6.2, 6.3, 7.7, 7.8_

- [x] 6. Controller 및 SuccessCode 확장
  - [x] 6.1 LoanDashboardSuccessCode에 LOAN_APPLICATION_REVIEW_OK 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/exception/LoanDashboardSuccessCode.java` 수정
    - `LOAN_APPLICATION_REVIEW_OK(HttpStatus.OK, "LOAN2006", "심사 결과 탭 조회에 성공했습니다.")` 추가
    - _Requirements: 1.3_

  - [x] 6.2 LoanDashboardController에 review 엔드포인트 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/controller/LoanDashboardController.java` 수정
    - `LoanApplicationReviewService` 의존성 주입 추가
    - `@GetMapping("/{applicationId}/review")` 엔드포인트 추가
    - `ApiResponse.onSuccess(LoanDashboardSuccessCode.LOAN_APPLICATION_REVIEW_OK, response)` 반환
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 6.3 LoanDashboardControllerDocs에 Swagger 문서 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/controller/LoanDashboardControllerDocs.java` 수정
    - `@Operation(summary = "대출 신청 상세 조회 (심사 결과 탭)")` 추가
    - 200, 401, 403, 404 응답 코드 문서화
    - _Requirements: 1.1, 2.1, 2.2, 3.1_

- [x] 7. Final checkpoint - 전체 통합 검증
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (jqwik 라이브러리 사용)
- Unit tests validate specific examples and edge cases (JUnit 5 + Mockito)
- 인증/권한 검증(Requirement 2)은 기존 Spring Security 필터에서 처리되므로 별도 구현 태스크 불필요
- LoanDecision의 `@OneToOne` → `@ManyToOne` 변경은 동일 application에 시스템 심사 + 은행원 심사가 모두 존재할 수 있기 때문

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "2.1"] },
    { "id": 1, "tasks": ["3.1"] },
    { "id": 2, "tasks": ["3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "5.1"] },
    { "id": 3, "tasks": ["5.2"] },
    { "id": 4, "tasks": ["5.3", "6.1"] },
    { "id": 5, "tasks": ["6.2", "6.3"] }
  ]
}
```
