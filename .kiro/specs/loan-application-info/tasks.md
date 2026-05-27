# Implementation Plan: 대출 신청 상세보기 정보 탭 API

## Overview

기존 `LoanDashboardController`에 `GET /api/admin/loan-applications/{applicationId}/info` 엔드포인트를 추가하고, 비즈니스 로직은 새로운 `LoanApplicationInfoService`로 분리하여 구현한다. Response DTO는 중첩 record 구조로 5개 섹션(applicantInfo, businessInfo, applicationInfo, userInputInfo, consentHistories)을 포함한다.

## Tasks

- [ ] 1. Response DTO 및 SuccessCode 생성
  - [x] 1.1 LoanApplicationInfoResponse record 생성
    - `com.sofit.admin.domain.loan.dto.response.LoanApplicationInfoResponse.java` 파일 생성
    - 중첩 record: ApplicantInfo, BusinessInfo, ApplicationInfo, UserInputInfo, ConsentHistoryItem
    - ApplicantInfo: name, residentNumber, phoneNumber, joinedAt(LocalDateTime), loginId
    - BusinessInfo: businessName, businessNumber, businessCategory, businessType, businessAddress, openDate(LocalDate)
    - ApplicationInfo: requestedAmount(Long), requestedTerm(Integer), purpose(String), repaymentMethod(String)
    - UserInputInfo: annualIncome, creditScore, incomeType, existingLoanAmount (모두 String)
    - ConsentHistoryItem: title, isRequired(Boolean), isConsented(Boolean), consentedAt(LocalDateTime)
    - _Requirements: 1.1, 2.2, 3.2, 4.1, 5.1, 6.2_

  - [x] 1.2 LoanDashboardSuccessCode에 LOAN_APPLICATION_INFO_OK 추가
    - `LoanDashboardSuccessCode.java`에 `LOAN_APPLICATION_INFO_OK(HttpStatus.OK, "LOAN2003", "대출 신청 정보 탭 조회에 성공했습니다.")` 추가
    - _Requirements: 1.3_

- [x] 2. Repository 메서드 추가 및 Converter 생성
  - [x] 2.1 ConsentHistoryRepository에 조회 메서드 추가
    - `sofit-common`의 `ConsentHistoryRepository.java`에 `findByUser_UserIdOrderByConsentIdAsc(Long userId)` 메서드 추가
    - 반환 타입: `List<ConsentHistory>`
    - consent_id 오름차순 정렬
    - _Requirements: 6.1, 6.6_

  - [x] 2.2 LoanApplicationInfoConverter 생성
    - `com.sofit.admin.domain.loan.converter.LoanApplicationInfoConverter.java` 파일 생성
    - private 생성자 (유틸리티 클래스)
    - `toLoanApplicationInfoResponse(User, BusinessProfile, LoanApplication, List<ConsentHistory>)` static 메서드
    - `toApplicantInfo(User)`: name, residentNumber, phoneNumber, createdAt→joinedAt, loginId 매핑
    - `toBusinessInfo(BusinessProfile)`: 6개 필드 직접 매핑 (null 허용 필드 그대로 전달)
    - `toApplicationInfo(LoanApplication)`: requestedAmount, requestedTerm, purpose.name(), repaymentMethod.name()
    - `toUserInputInfo(LoanApplication)`: annualIncome.name(), creditScore.name(), incomeType.getCode(), existingLoanAmt.name() (null 체크 포함)
    - `toConsentHistories(List<ConsentHistory>)`: 빈 리스트 처리, stream 변환
    - `toConsentHistoryItem(ConsentHistory)`: title, isRequired, isConsented, consentedAt(isConsented=false면 null)
    - _Requirements: 2.1, 2.2, 3.1, 3.2, 3.4, 4.1, 4.2, 4.3, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 6.2, 6.3, 6.5_

- [x] 3. Service 인터페이스 및 구현체 생성
  - [x] 3.1 LoanApplicationInfoService 인터페이스 생성
    - `com.sofit.admin.domain.loan.service.LoanApplicationInfoService.java` 파일 생성
    - `LoanApplicationInfoResponse findLoanApplicationInfo(Long applicationId)` 메서드 선언
    - _Requirements: 1.1_

  - [x] 3.2 LoanApplicationInfoServiceImpl 구현체 생성
    - `com.sofit.admin.domain.loan.service.LoanApplicationInfoServiceImpl.java` 파일 생성
    - `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)` 어노테이션
    - LoanApplicationRepository, UserRepository, BusinessProfileRepository, ConsentHistoryRepository 주입
    - findLoanApplicationInfo 구현:
      1. `loanApplicationRepository.findById(applicationId)` → 없으면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      2. `userRepository.findById(userId)` → 없으면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      3. `businessProfileRepository.findByUser_UserId(userId)` → 없으면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      4. `consentHistoryRepository.findByUser_UserIdOrderByConsentIdAsc(userId)`
      5. `LoanApplicationInfoConverter.toLoanApplicationInfoResponse(...)` 호출하여 반환
    - _Requirements: 1.1, 2.1, 2.4, 3.1, 6.1, 7.1_

- [x] 4. Checkpoint - 핵심 로직 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Controller 및 ControllerDocs 확장
  - [x] 5.1 LoanDashboardControllerDocs에 Swagger 메서드 추가
    - 기존 `LoanDashboardControllerDocs.java`에 `findLoanApplicationInfo` 메서드 추가
    - `@Operation(summary = "대출 신청 상세 조회 (정보 탭)")` 어노테이션
    - `@ApiResponses`: 200(정보 탭 조회 성공), 404(대출 신청 건을 찾을 수 없음)
    - 파라미터: `@Parameter(description = "대출 신청 ID", example = "1") Long applicationId`
    - 반환 타입: `ApiResponse<LoanApplicationInfoResponse>`
    - _Requirements: 1.1, 1.2_

  - [x] 5.2 LoanDashboardController에 엔드포인트 추가
    - 기존 `LoanDashboardController.java`에 `LoanApplicationInfoService` 의존성 주입 추가
    - `@GetMapping("/{applicationId}/info")` 메서드 추가
    - `@Override` + `@PathVariable Long applicationId`
    - Service 호출 후 `ApiResponse.onSuccess(LoanDashboardSuccessCode.LOAN_APPLICATION_INFO_OK, response)` 반환
    - _Requirements: 1.1, 1.2, 1.3_

- [x] 6. Checkpoint - 전체 통합 검증
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 7. Property-Based 테스트 작성
  - [ ]* 7.1 Property 1 테스트: applicantInfo 변환 정확성
    - **Property 1: applicantInfo 변환 정확성**
    - 임의의 User에 대해 toApplicantInfo 결과가 원본 필드와 일치하는지 검증
    - **Validates: Requirements 2.1, 2.2**

  - [ ]* 7.2 Property 2 테스트: businessInfo 변환 정확성
    - **Property 2: businessInfo 변환 정확성**
    - 임의의 BusinessProfile에 대해 toBusinessInfo 결과가 원본 필드와 일치하는지 검증
    - **Validates: Requirements 3.1, 3.2**

  - [ ]* 7.3 Property 3 테스트: applicationInfo 변환 정확성
    - **Property 3: applicationInfo 변환 정확성**
    - 임의의 LoanApplication에 대해 toApplicationInfo 결과가 ENUM.name() 규칙을 따르는지 검증
    - **Validates: Requirements 4.1, 4.2, 4.3**

  - [ ]* 7.4 Property 4 테스트: userInputInfo 변환 정확성
    - **Property 4: userInputInfo 변환 정확성 (incomeType code 변환 포함)**
    - 임의의 LoanApplication에 대해 toUserInputInfo 결과가 name()/getCode() 규칙을 따르는지 검증
    - **Validates: Requirements 5.1, 5.2, 5.3, 5.4, 5.5**

  - [ ]* 7.5 Property 5+6 테스트: consentHistories 변환 정확성 및 미동의 시 null 처리
    - **Property 5: consentHistories 변환 정확성**
    - **Property 6: 미동의 시 consentedAt null 처리**
    - 임의의 ConsentHistory+Term 목록에 대해 변환 결과가 원본과 일치하고, isConsented=false면 consentedAt이 null인지 검증
    - **Validates: Requirements 6.1, 6.2, 6.3**

  - [ ]* 7.6 Property 7 테스트: consentHistories 정렬 보장
    - **Property 7: consentHistories 정렬 보장**
    - 임의 순서의 ConsentHistory 목록에 대해 Repository 쿼리 결과가 consent_id 오름차순인지 검증
    - **Validates: Requirements 6.6**

- [ ]* 8. 단위 테스트 작성
  - [ ]* 8.1 LoanApplicationInfoServiceImpl 단위 테스트
    - 정상 조회 시 5개 섹션 포함 응답 반환 검증
    - 존재하지 않는 applicationId → BaseException(NOT_FOUND) 검증
    - User 미존재 → BaseException(NOT_FOUND) 검증
    - BusinessProfile 미존재 → BaseException(NOT_FOUND) 검증
    - ConsentHistory 0건 → consentHistories 빈 배열 검증
    - _Requirements: 1.1, 1.4, 2.4, 7.1_

  - [ ]* 8.2 LoanApplicationInfoConverter 단위 테스트
    - 정상 BusinessProfile → businessInfo 변환 정확성 검증
    - isConsented=false → consentedAt null 검증
    - 선택적 필드 null → null 유지 검증
    - userInputInfo ENUM null → null 반환 검증
    - _Requirements: 3.4, 5.6, 6.3_

- [x] 9. Final Checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 기존 파일(LoanDashboardController, LoanDashboardControllerDocs, LoanDashboardSuccessCode, ConsentHistoryRepository)에는 메서드/필드 추가만 수행
- 인증/권한 검증(Requirements 7.2, 7.3, 8.1, 8.2, 8.3)은 기존 Spring Security 필터 체인에서 처리되므로 별도 구현 불필요
- applicationId 형식 오류(Requirements 7.2)는 기존 GlobalExceptionHandler에서 MethodArgumentTypeMismatchException으로 처리됨
- Property-Based 테스트는 jqwik 라이브러리 사용
- BusinessProfile은 대출 신청 전제조건이므로 null 방어 불필요 (설계 결정 사항)

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "2.1"] },
    { "id": 1, "tasks": ["2.2", "3.1"] },
    { "id": 2, "tasks": ["3.2"] },
    { "id": 3, "tasks": ["5.1"] },
    { "id": 4, "tasks": ["5.2"] },
    { "id": 5, "tasks": ["7.1", "7.2", "7.3", "7.4", "7.5", "7.6"] },
    { "id": 6, "tasks": ["8.1", "8.2"] }
  ]
}
```
