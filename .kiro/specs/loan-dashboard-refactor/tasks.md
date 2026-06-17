# Implementation Plan: Loan Dashboard Refactor

## Overview

기존 대출 대시보드 API(`GET /api/admin/loan-applications`)를 수정한다. `assigneeName`/`assignedBankerId` 쿼리 파라미터를 삭제하고 `myOnly` Boolean 파라미터로 대체하며, 응답 필드명을 `applications`에서 `contents`로 변경한다. SecurityContext에서 현재 로그인한 은행원의 userId를 추출하여 본인 담당 건 필터링을 구현한다.

## Tasks

- [x] 1. DTO 및 Converter 수정 (응답 필드명 변경)
  - [x] 1.1 LoanDashboardResponse 레코드의 필드명을 applications → contents로 변경
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/dto/response/LoanDashboardResponse.java` 수정
    - 필드명만 변경, 타입은 `List<LoanApplicationItemResponse>` 유지
    - _Requirements: 1.2, 1.4_

  - [x] 1.2 LoanDashboardConverter에서 필드명 변경 반영
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/converter/LoanDashboardConverter.java` 수정
    - `toLoanDashboardResponse` 메서드에서 contents 필드에 매핑
    - BusinessProfile 최신 선택 로직 추가 (userId별 createdAt 최신 1건의 businessName 선택)
    - _Requirements: 1.2, 4.2, 4.3_

- [x] 2. Service 레이어 수정 (myOnly 로직 구현)
  - [x] 2.1 LoanDashboardService 인터페이스 시그니처 변경
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/LoanDashboardService.java` 수정
    - 메서드 시그니처: `findLoanApplications(ApplicationStatus status, Boolean myOnly, Long currentUserId, Pageable pageable)`
    - _Requirements: 3.1, 3.3, 3.4_

  - [x] 2.2 LoanDashboardServiceImpl에서 myOnly 분기 로직 구현
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/LoanDashboardServiceImpl.java` 수정
    - myOnly=true → `findDashboardApplicationsByBankerId(statuses, currentUserId, pageable)` 호출
    - myOnly=false/null → `findDashboardApplications(statuses, pageable)` 호출
    - status가 null이면 DASHBOARD_STATUSES 전체, 아니면 단일 상태 List로 변환
    - BusinessProfile 일괄 조회 후 userId별 최신 createdAt 선택
    - bankerNameMap 일괄 조회
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.4, 4.5, 4.6, 4.7_

- [x] 3. Controller 레이어 수정 (파라미터 변경 및 SecurityContext 연동)
  - [x] 3.1 LoanDashboardController 수정
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/controller/LoanDashboardController.java` 수정
    - `assignedBankerId`, `assigneeName` @RequestParam 제거
    - `myOnly` (Boolean, defaultValue="false") @RequestParam 추가
    - `status`를 단일 String으로 변경
    - page/size 유효성 검증 (page < 0, size < 1 || size > 100)
    - status 파라미터 검증 (ApplicationStatus.valueOf + ALLOWED_STATUSES 체크)
    - `extractCurrentUserId()` private 메서드 추가 (SecurityContextHolder에서 userId 추출)
    - Service 호출 시 applicationStatus, myOnly, currentUserId, pageable 전달
    - _Requirements: 1.1, 1.5, 1.6, 1.7, 1.8, 2.1, 2.2, 2.3, 3.1, 3.4, 3.7, 5.1, 6.1, 6.2, 6.3, 6.4_

  - [x] 3.2 LoanDashboardControllerDocs 인터페이스 수정
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/controller/LoanDashboardControllerDocs.java` 수정
    - Swagger @Operation, @Parameter 어노테이션 업데이트
    - assignedBankerId/assigneeName 파라미터 제거, myOnly 파라미터 추가
    - _Requirements: 6.3, 6.4_

- [x] 4. Checkpoint - 컴파일 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. 단위 테스트 작성
  - [ ]* 5.1 LoanDashboardConverter 단위 테스트 작성
    - `sofit-admin/src/test/java/com/sofit/admin/domain/loan/converter/LoanDashboardConverterTest.java` 생성
    - Entity → DTO 변환 정확성 검증 (필드 매핑)
    - null 필드 처리 (assigneeName=null, businessName=null)
    - 페이징 메타데이터 변환 정확성
    - contents 필드명 변경 확인
    - _Requirements: 1.2, 1.4, 4.3, 4.6_

  - [ ]* 5.2 LoanDashboardServiceImpl 단위 테스트 작성
    - `sofit-admin/src/test/java/com/sofit/admin/domain/loan/service/LoanDashboardServiceImplTest.java` 생성
    - myOnly=true 시 findDashboardApplicationsByBankerId 호출 검증
    - myOnly=false 시 findDashboardApplications 호출 검증
    - status 단일 필터 적용/미적용 시 동작 검증
    - BusinessProfile 최신 선택 로직 검증
    - 빈 결과 시 응답 구조 검증
    - _Requirements: 3.1, 3.2, 3.3, 3.5, 4.2, 4.7_

  - [ ]* 5.3 LoanDashboardController MockMvc 테스트 작성
    - `sofit-admin/src/test/java/com/sofit/admin/domain/loan/controller/LoanDashboardControllerTest.java` 생성
    - 정상 요청 시 200 응답 및 공통 포맷 검증
    - myOnly=true 시 SecurityContext userId 전달 검증
    - 잘못된 status 파라미터 시 400 에러 응답
    - page 음수, size 범위 초과 시 400 에러 응답
    - 기본값 적용 검증 (page=0, size=10, myOnly=false)
    - 기존 assignedBankerId/assigneeName 파라미터 전달 시 무시 확인
    - _Requirements: 1.5, 1.6, 1.7, 1.8, 2.3, 3.4, 3.7, 5.1, 5.3, 5.4, 6.1, 6.2_

- [ ] 6. 속성 기반 테스트 (Property-Based Tests)
  - [ ]* 6.1 Property 1: 상태 필터링 및 정렬 보장 테스트
    - **Property 1: 상태 필터링 및 정렬 보장**
    - **Validates: Requirements 1.1, 2.1, 2.2**
    - jqwik 라이브러리 사용, 임의의 ApplicationStatus + LoanApplication 목록 생성
    - 결과의 모든 항목이 허용된 상태만 포함하고 appliedAt 내림차순 정렬 검증

  - [ ]* 6.2 Property 2: Converter 메타데이터 매핑 정확성 테스트
    - **Property 2: Converter 메타데이터 매핑 정확성**
    - **Validates: Requirements 1.2**
    - 임의의 Page 메타데이터(totalElements, totalPages, number, size) 생성
    - 변환된 LoanDashboardResponse의 메타데이터가 원본 Page와 일치 검증

  - [ ]* 6.3 Property 3: Converter 항목 필드 매핑 정확성 테스트
    - **Property 3: Converter 항목 필드 매핑 정확성**
    - **Validates: Requirements 1.4, 4.1, 4.4, 4.5**
    - 임의의 LoanApplication + User + LoanProduct + Map 조합 생성
    - 변환된 LoanApplicationItemResponse 필드가 원본과 일치 검증

  - [ ]* 6.4 Property 4: myOnly 필터링 정확성 테스트
    - **Property 4: myOnly 필터링 정확성**
    - **Validates: Requirements 3.1, 3.2, 3.6**
    - 임의의 userId + LoanApplication 목록 생성
    - myOnly=true 결과의 모든 항목이 assignedBankerId == userId 검증

  - [ ]* 6.5 Property 5: 잘못된 status 값 에러 처리 테스트
    - **Property 5: 잘못된 status 값 에러 처리**
    - **Validates: Requirements 2.3, 5.3**
    - 허용되지 않은 임의의 문자열 생성, 에러 응답 검증

  - [ ]* 6.6 Property 6: 최신 BusinessProfile 선택 테스트
    - **Property 6: 최신 BusinessProfile 선택**
    - **Validates: Requirements 4.2**
    - 임의의 User에 대해 여러 BusinessProfile(다양한 createdAt) 생성
    - 반환되는 businessName이 createdAt 최신 레코드와 일치 검증

- [x] 7. Final Checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 기존 Repository 메서드(`findDashboardApplications`, `findDashboardApplicationsByBankerId`)를 그대로 재활용하므로 Repository 변경 불필요
- LoanApplicationItemResponse는 변경 없음 (appliedAt은 LocalDateTime 유지)
- 속성 기반 테스트는 jqwik 라이브러리 사용 (JUnit 5 호환)
- 각 태스크는 이전 태스크의 결과물 위에 점진적으로 구현
- Checkpoints에서 컴파일 오류 및 테스트 통과 여부 확인

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2", "2.2"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["5.1", "5.2", "5.3"] },
    { "id": 4, "tasks": ["6.1", "6.2", "6.3", "6.4", "6.5", "6.6"] }
  ]
}
```
