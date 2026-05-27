# Implementation Plan: Loan Dashboard

## Overview

은행원(BANK_ADMIN)이 대출 신청 현황을 페이징 조회할 수 있는 대시보드 API를 구현한다. sofit-common에 Repository 메서드를 추가하고, sofit-admin/domain/loan/ 하위에 Controller, Service, Converter, DTO, Exception 클래스를 생성한다. 기존 프로젝트 컨벤션(ControllerDocs 인터페이스, Service interface + Impl, static Converter, record Response DTO, ApiResponse 공통 응답)을 준수한다.

## Tasks

- [x] 1. sofit-common Repository 메서드 추가
  - [x] 1.1 LoanApplicationRepository에 JPQL 대시보드 조회 메서드 2개 추가
    - `findDashboardApplications(statuses, pageable)` — status 필터만 적용, JOIN FETCH (User, LoanProduct) + JOIN (Banker), countQuery 별도 지정
    - `findDashboardApplicationsByAssigneeName(statuses, assigneeName, pageable)` — status + assigneeName 필터 적용, countQuery에도 banker JOIN 포함
    - ORDER BY la.appliedAt DESC
    - _Requirements: 1.1, 2.1, 2.2, 3.1, 4.1, 4.3, 4.4_

  - [x] 1.2 BusinessProfileRepository에 findByUser_UserIdIn 메서드 추가
    - `List<BusinessProfile> findByUser_UserIdIn(List<Long> userIds)` — 페이지 내 userIds로 일괄 조회
    - _Requirements: 4.2_

- [x] 2. sofit-admin Exception 레이어 생성
  - [x] 2.1 LoanDashboardSuccessCode enum 생성
    - `LOAN_DASHBOARD_OK(HttpStatus.OK, "LOAN2001", "대출 신청 목록 조회에 성공했습니다.")` — BaseSuccessCode implements
    - 파일: `sofit-admin/domain/loan/exception/LoanDashboardSuccessCode.java`
    - _Requirements: 5.1_

  - [x] 2.2 LoanDashboardErrorCode enum 생성
    - `INVALID_STATUS_FILTER(HttpStatus.BAD_REQUEST, "LOAN4001", "유효하지 않은 심사 상태입니다.")` — BaseErrorCode implements
    - 파일: `sofit-admin/domain/loan/exception/LoanDashboardErrorCode.java`
    - _Requirements: 2.3, 5.3_

- [x] 3. sofit-admin DTO 레이어 생성
  - [x] 3.1 LoanApplicationItemResponse record 생성
    - 필드: applicationId(Long), appliedAt(LocalDateTime), applicantName(String), businessName(String), productName(String), status(ApplicationStatus), assignedBankerId(Long), assigneeName(String)
    - 파일: `sofit-admin/domain/loan/dto/response/LoanApplicationItemResponse.java`
    - _Requirements: 1.4_

  - [x] 3.2 LoanDashboardResponse record 생성
    - 필드: totalCount(long), totalPages(int), currentPage(int), size(int), applications(List<LoanApplicationItemResponse>)
    - 파일: `sofit-admin/domain/loan/dto/response/LoanDashboardResponse.java`
    - _Requirements: 1.2, 1.3_

- [x] 4. sofit-admin Converter 레이어 생성
  - [x] 4.1 LoanDashboardConverter 클래스 생성
    - private 생성자 (인스턴스화 방지)
    - `static toLoanDashboardResponse(Page<LoanApplication>, Map<Long, String> businessNameMap, Map<Long, String> bankerNameMap)` → LoanDashboardResponse
    - `static toLoanApplicationItemResponse(LoanApplication, String businessName, String assigneeName)` → LoanApplicationItemResponse
    - applicantName: app.getUser().getName() (JOIN FETCH로 로딩됨)
    - productName: app.getProduct().getProductName() (JOIN FETCH로 로딩됨)
    - 파일: `sofit-admin/domain/loan/converter/LoanDashboardConverter.java`
    - _Requirements: 1.4, 4.1, 4.2, 4.3, 4.4_

  - [ ]* 4.2 LoanDashboardConverter 단위 테스트 작성
    - **Property 2: Converter 매핑 정확성**
    - **Validates: Requirements 1.4, 4.1, 4.3, 4.4**

- [x] 5. sofit-admin Service 레이어 생성
  - [x] 5.1 LoanDashboardService 인터페이스 생성
    - `LoanDashboardResponse findLoanApplications(ApplicationStatus status, String assigneeName, Pageable pageable)`
    - 파일: `sofit-admin/domain/loan/service/LoanDashboardService.java`
    - _Requirements: 1.1, 2.1, 2.2, 3.1, 3.2_

  - [x] 5.2 LoanDashboardServiceImpl 구현체 생성
    - DASHBOARD_STATUSES 상수 정의 (SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED)
    - status null이면 5개 상태 전체 조회, 아니면 해당 상태만 List로 감싸서 조회
    - assigneeName null 또는 빈 문자열이면 findDashboardApplications 호출, 아니면 findDashboardApplicationsByAssigneeName 호출
    - 조회된 Page에서 userIds 추출 → BusinessProfileRepository.findByUser_UserIdIn으로 일괄 조회 → Map<Long, String> 변환
    - bankerNameMap: assignedBankerId → Users 테이블에서 name 조회 (JPQL JOIN으로 이미 로딩된 banker 정보 활용)
    - LoanDashboardConverter.toLoanDashboardResponse 호출하여 반환
    - 파일: `sofit-admin/domain/loan/service/LoanDashboardServiceImpl.java`
    - _Requirements: 1.1, 1.2, 1.3, 2.1, 2.2, 3.1, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4_

  - [ ]* 5.3 LoanDashboardServiceImpl 단위 테스트 작성
    - **Property 1: 상태 필터링 및 정렬 보장**
    - **Property 4: 담당자명 exact match 필터링**
    - **Validates: Requirements 1.1, 2.1, 2.2, 3.1**

- [x] 6. Checkpoint - 중간 점검
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. sofit-admin Controller 레이어 생성
  - [x] 7.1 LoanDashboardControllerDocs 인터페이스 생성
    - @Tag(name = "대출 대시보드")
    - @Operation, @ApiResponses 어노테이션으로 Swagger 문서 정의
    - 메서드 시그니처: `ApiResponse<LoanDashboardResponse> findLoanApplications(Integer page, Integer size, String status, String assigneeName)`
    - 파일: `sofit-admin/domain/loan/controller/LoanDashboardControllerDocs.java`
    - _Requirements: 5.1, 5.2, 5.3_

  - [x] 7.2 LoanDashboardController 구현체 생성
    - @RestController, @RequestMapping("/api/admin/loan-applications")
    - implements LoanDashboardControllerDocs
    - GET 메서드: @RequestParam으로 page(default 0), size(default 10), status(optional), assigneeName(optional) 수신
    - status String → ApplicationStatus 변환 시 유효성 검증 (유효하지 않으면 BaseException(LoanDashboardErrorCode.INVALID_STATUS_FILTER) throw)
    - 허용 상태 목록(SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED) 외 값이면 에러
    - PageRequest.of(page, size) 생성하여 Service 호출
    - ApiResponse.onSuccess(LoanDashboardSuccessCode.LOAN_DASHBOARD_OK, response) 반환
    - 파일: `sofit-admin/domain/loan/controller/LoanDashboardController.java`
    - _Requirements: 1.1, 1.5, 1.6, 2.1, 2.2, 2.3, 3.1, 3.2, 5.1, 5.2, 5.3_

  - [ ]* 7.3 LoanDashboardController MockMvc 테스트 작성
    - **Property 5: 잘못된 상태 값 에러 처리**
    - **Validates: Requirements 2.3, 5.3**

- [x] 8. Final checkpoint - 최종 점검
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 구현 언어: Java (Spring Boot)
- sofit-common 모듈: Repository 메서드 추가만 (1.1, 1.2)
- sofit-admin 모듈: 나머지 모든 구현 (2~7)
- 기존 .gitkeep 파일은 실제 파일 생성 시 삭제
- assignedBankerId, assigneeName, businessName은 모두 NOT NULL — null 체크 불필요
- ApplicationStatus enum은 이미 확장 완료 (SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW 포함)
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "2.1", "2.2", "3.1", "3.2"] },
    { "id": 1, "tasks": ["4.1", "5.1"] },
    { "id": 2, "tasks": ["4.2", "5.2"] },
    { "id": 3, "tasks": ["5.3", "7.1"] },
    { "id": 4, "tasks": ["7.2"] },
    { "id": 5, "tasks": ["7.3"] }
  ]
}
```
