# Implementation Plan: 대출 신청 상세 조회 API

## Overview

기존 `LoanDashboardController`에 `GET /{applicationId}` 엔드포인트를 추가하여 대출 신청 건의 공통 정보를 단건 조회하는 API를 구현한다. 기존 프로젝트 아키텍처 패턴(Controller + ControllerDocs, Service interface + ServiceImpl, Converter, Response DTO record)을 그대로 따른다.

## Tasks

- [x] 1. Response DTO 및 Exception 코드 정의
  - [x] 1.1 LoanApplicationDetailResponse record 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/dto/response/LoanApplicationDetailResponse.java` 파일 생성
    - applicationId(Long), applicantName(String), businessName(String), productName(String), status(String), appliedAt(String), assignedBankerId(Long), assigneeName(String) 필드 정의
    - assignedBankerId, assigneeName은 nullable
    - _Requirements: 1.2_

  - [x] 1.2 LoanDashboardErrorCode에 LOAN_APPLICATION_NOT_FOUND 추가
    - `LoanDashboardErrorCode.java`에 `LOAN_APPLICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON4004", "요청한 리소스를 찾을 수 없습니다.")` 추가
    - _Requirements: 2.1, 3.5_

  - [x] 1.3 LoanDashboardSuccessCode에 LOAN_APPLICATION_DETAIL_OK 추가
    - `LoanDashboardSuccessCode.java`에 `LOAN_APPLICATION_DETAIL_OK(HttpStatus.OK, "LOAN2002", "대출 신청 상세 조회에 성공했습니다.")` 추가
    - _Requirements: 3.4_

- [x] 2. Converter 및 Service 구현
  - [x] 2.1 LoanDashboardConverter에 toLoanApplicationDetailResponse 메서드 추가
    - `LoanDashboardConverter.java`에 static 메서드 추가
    - LoanApplication, businessName(String), assigneeName(String)을 인자로 받아 LoanApplicationDetailResponse로 변환
    - appliedAt은 `yyyy-MM-dd HH:mm:ss` 포맷 문자열로 변환 (null인 경우 null 반환)
    - status는 `ApplicationStatus.name()` 문자열로 변환
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 3.2_

  - [ ]* 2.2 Converter 필드 매핑 정확성 Property 테스트 작성
    - **Property 1: Converter 필드 매핑 정확성**
    - **Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.6, 1.7**
    - jqwik 라이브러리를 사용하여 임의의 LoanApplication, businessName, assigneeName 조합에 대해 toLoanApplicationDetailResponse가 모든 필드를 정확하게 매핑하는지 검증

  - [x] 2.3 LoanDashboardService 인터페이스에 findLoanApplicationDetail 메서드 추가
    - `LoanDashboardService.java`에 `LoanApplicationDetailResponse findLoanApplicationDetail(Long applicationId);` 시그니처 추가
    - _Requirements: 1.1, 3.1_

  - [x] 2.4 LoanDashboardServiceImpl에 findLoanApplicationDetail 구현
    - `LoanDashboardServiceImpl.java`에 메서드 구현
    - loanApplicationRepository.findById(applicationId)로 조회, 없으면 BaseException(LOAN_APPLICATION_NOT_FOUND) throw
    - businessProfileRepository.findByUser_UserId(userId)로 businessName 조회
    - assignedBankerId가 null이 아닌 경우 userRepository.findById(assignedBankerId)로 assigneeName 조회
    - LoanDashboardConverter.toLoanApplicationDetailResponse로 DTO 변환 후 반환
    - _Requirements: 1.1, 1.3, 1.4, 1.5, 1.6, 1.7, 2.1, 3.2_

  - [ ]* 2.5 Service 단위 테스트 작성
    - 정상 조회 (assignedBankerId 존재): 모든 필드 올바르게 반환 검증
    - 정상 조회 (assignedBankerId null): assigneeName이 null로 반환 검증
    - 존재하지 않는 applicationId: BaseException(LOAN_APPLICATION_NOT_FOUND) throw 검증
    - _Requirements: 1.1, 1.6, 1.7, 2.1_

- [x] 3. Checkpoint - 중간 점검
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Controller 및 Swagger 문서 연동
  - [x] 4.1 LoanDashboardControllerDocs에 findLoanApplicationDetail 메서드 추가
    - `LoanDashboardControllerDocs.java`에 Swagger 어노테이션(@Operation, @ApiResponses, @Parameter) 포함한 메서드 시그니처 추가
    - summary: "대출 신청 상세 조회 (공통 정보)", description: "대출 신청 건의 공통 정보를 단건 조회합니다."
    - 200 성공, 404 미존재 응답 코드 문서화
    - _Requirements: 3.3_

  - [x] 4.2 LoanDashboardController에 GET /{applicationId} 엔드포인트 추가
    - `LoanDashboardController.java`에 `@GetMapping("/{applicationId}")` 메서드 추가
    - `@PathVariable Long applicationId`로 경로 변수 바인딩
    - loanDashboardService.findLoanApplicationDetail(applicationId) 호출
    - ApiResponse.onSuccess(LoanDashboardSuccessCode.LOAN_APPLICATION_DETAIL_OK, response) 반환
    - ControllerDocs 인터페이스의 메서드를 @Override
    - _Requirements: 1.1, 3.1, 3.4_

  - [ ]* 4.3 통합 테스트 작성 (MockMvc)
    - 정상 조회: GET /api/admin/loan-applications/{applicationId} → 200 + ApiResponse 형식 검증
    - 존재하지 않는 ID: → 404 + 에러 응답 형식 검증
    - 잘못된 ID 형식 (문자열): → 400 응답 검증
    - _Requirements: 1.1, 2.1, 2.2, 2.3_

- [x] 5. Final checkpoint - 최종 점검
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (jqwik 라이브러리 사용)
- Unit tests validate specific examples and edge cases (JUnit 5 + Mockito)
- 기존 파일 수정 시 기존 코드를 유지하면서 메서드/enum 값만 추가하는 방식으로 구현

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["2.1", "2.3"] },
    { "id": 2, "tasks": ["2.2", "2.4"] },
    { "id": 3, "tasks": ["2.5", "4.1"] },
    { "id": 4, "tasks": ["4.2"] },
    { "id": 5, "tasks": ["4.3"] }
  ]
}
```
