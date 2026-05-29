# Implementation Plan: 지점장 결재 조회 API

## Overview

지점장(ADMIN_BANK_MANAGER) 또는 개발자(ADMIN_DEV) 역할을 가진 관리자가 `GET /api/admin/manager/loan-applications` 엔드포인트를 통해 MANAGER_REVIEW 상태의 대출 신청 건 목록을 조회하는 API를 구현한다. 기존 프로젝트의 패턴(Converter, SuccessCode, ControllerDocs 인터페이스 분리 등)을 따르며, AdminRoleService를 재사용 가능한 유틸리티로 생성한다.

## Tasks

- [x] 1. 핵심 인프라 및 DTO 생성
  - [x] 1.1 Response DTO 생성 (ManagerApprovalListResponse, ManagerApprovalItemResponse)
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/dto/response/` 경로에 record 타입으로 생성
    - ManagerApprovalListResponse: `List<ManagerApprovalItemResponse> applications` 필드
    - ManagerApprovalItemResponse: id, applicationDate, applicantName, businessName, productName, requestedByName, requestedAmount 필드
    - _Requirements: 1.2, 3.1~3.11_

  - [x] 1.2 ManagerApprovalSuccessCode enum 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/exception/` 경로에 생성
    - BaseSuccessCode 인터페이스 구현
    - MANAGER_APPROVAL_LIST_OK(HttpStatus.OK, "COMMON2000", "성공입니다.") 정의
    - _Requirements: 1.4_

  - [x] 1.3 AdminRoleService 생성
    - `sofit-admin/src/main/java/com/sofit/admin/global/util/` 경로에 생성
    - @Service, @RequiredArgsConstructor 적용
    - SecurityUtil.getCurrentUserId()로 userId 추출 후 UserRepository.findById()로 User 조회
    - INACTIVE 사용자 예외 처리 (AdminAuthErrorCode.USER_NOT_FOUND)
    - UserRole 반환하는 getCurrentUserRole() 메서드 구현
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 2. Repository 및 Converter 구현
  - [x] 2.1 LoanApplicationRepository에 조회 메서드 추가
    - `sofit-common/src/main/java/com/sofit/common/repository/LoanApplicationRepository.java` 수정
    - JPQL JOIN FETCH (user, product) + status 파라미터 + ORDER BY appliedAt ASC
    - 메서드명: `findByStatusWithUserAndProduct(@Param("status") ApplicationStatus status)`
    - _Requirements: 1.1_

  - [x] 2.2 ManagerApprovalConverter 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/converter/` 경로에 생성
    - private 생성자로 인스턴스화 방지 (기존 LoanDashboardConverter 패턴 따름)
    - toManagerApprovalListResponse(): List<LoanApplication> + businessNameMap + bankerNameMap → ManagerApprovalListResponse
    - toManagerApprovalItemResponse(): 개별 항목 변환 (appliedAt null 처리 → "yyyy-MM-dd" 형식)
    - _Requirements: 3.1~3.11_

  - [ ]* 2.3 ManagerApprovalConverter 프로퍼티 기반 테스트 작성
    - **Property 2: Converter 매핑 정확성**
    - **Validates: Requirements 1.2, 3.1~3.11**
    - jqwik 사용, 임의의 LoanApplication + nullable 필드 조합 생성
    - id, applicationDate, applicantName, businessName, productName, requestedByName, requestedAmount 매핑 정확성 검증

- [x] 3. Service 계층 구현
  - [x] 3.1 ManagerApprovalService 인터페이스 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/` 경로에 생성
    - `ManagerApprovalListResponse findManagerReviewApplications()` 메서드 정의
    - _Requirements: 1.1_

  - [x] 3.2 ManagerApprovalServiceImpl 구현
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/` 경로에 생성
    - @Service, @RequiredArgsConstructor, @Transactional(readOnly = true) 적용
    - LoanApplicationRepository, BusinessProfileRepository, UserRepository 주입
    - findByStatusWithUserAndProduct(MANAGER_REVIEW) 호출
    - BusinessProfile 일괄 조회 → userId별 businessName 매핑 (기존 LoanDashboardServiceImpl 패턴 따름)
    - assignedBankerId 일괄 조회 → bankerName 매핑
    - ManagerApprovalConverter.toManagerApprovalListResponse() 호출하여 반환
    - 빈 결과 시 빈 리스트 포함 응답 반환
    - _Requirements: 1.1, 1.3, 3.4, 3.6, 3.8, 3.9_

  - [ ]* 3.3 ManagerApprovalServiceImpl 프로퍼티 기반 테스트 작성
    - **Property 1: MANAGER_REVIEW 필터링 및 정렬 불변성**
    - **Validates: Requirements 1.1**
    - jqwik 사용, 다양한 ApplicationStatus를 가진 LoanApplication 리스트 생성
    - 결과가 MANAGER_REVIEW 상태만 포함하고 appliedAt 오름차순 정렬인지 검증

- [x] 4. Checkpoint - 중간 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Controller 계층 구현
  - [x] 5.1 ManagerApprovalControllerDocs 인터페이스 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/controller/` 경로에 생성
    - @Tag(name = "지점장 결재") 적용
    - @Operation(summary = "지점장 결재 대기 목록 조회") 적용
    - ApiResponse<ManagerApprovalListResponse> findManagerApprovalList() 메서드 선언
    - _Requirements: 1.1_

  - [x] 5.2 ManagerApprovalController 구현
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/controller/` 경로에 생성
    - @RestController, @RequestMapping("/api/admin/manager"), @RequiredArgsConstructor 적용
    - ManagerApprovalControllerDocs 인터페이스 구현
    - AdminRoleService, ManagerApprovalService 주입
    - @GetMapping("/loan-applications") 매핑
    - AdminRoleService.getCurrentUserRole()로 역할 조회
    - ADMIN_BANK_MANAGER, ADMIN_DEV가 아니면 BaseException(GeneralErrorCode.FORBIDDEN) throw
    - 정상 시 ManagerApprovalService.findManagerReviewApplications() 호출
    - ApiResponse.onSuccess(ManagerApprovalSuccessCode.MANAGER_APPROVAL_LIST_OK, response) 반환
    - _Requirements: 1.1, 1.4, 2.1, 2.2, 2.3, 2.4_

  - [ ]* 5.3 Controller 단위 테스트 작성
    - MockMvc + @WebMvcTest 사용
    - 허용 역할(ADMIN_BANK_MANAGER, ADMIN_DEV) 정상 처리 테스트
    - 비허용 역할(USER, ADMIN_BANK_TELLER) 403 응답 테스트
    - 미인증 401 응답 테스트
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [ ] 6. 단위 테스트 보강
  - [ ]* 6.1 AdminRoleService 단위 테스트 작성
    - Mockito 사용
    - 정상 역할 반환 테스트
    - 미인증(SecurityContext 비어있음) 예외 테스트
    - 사용자 미존재 예외 테스트
    - INACTIVE 사용자 예외 테스트
    - _Requirements: 4.1, 4.2, 4.3_

  - [ ]* 6.2 ManagerApprovalServiceImpl 단위 테스트 작성
    - Mockito 사용
    - 빈 결과 시 빈 리스트 반환 테스트
    - 정상 데이터 변환 테스트 (businessName null, bankerName null 케이스 포함)
    - _Requirements: 1.1, 1.3, 3.8, 3.9_

- [x] 7. Final checkpoint - 최종 검증
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 태스크는 특정 요구사항을 참조하여 추적 가능
- Checkpoint에서 점진적 검증 수행
- Property 테스트는 보편적 정확성 속성을 검증하고, 단위 테스트는 특정 예제와 엣지 케이스를 검증
- 기존 프로젝트 패턴(LoanDashboardConverter, LoanDashboardSuccessCode 등)을 최대한 따름
- AdminRoleService는 global/util/ 패키지에 위치하여 다른 도메인에서도 재사용 가능

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "2.1"] },
    { "id": 1, "tasks": ["2.2", "3.1"] },
    { "id": 2, "tasks": ["2.3", "3.2"] },
    { "id": 3, "tasks": ["3.3", "5.1"] },
    { "id": 4, "tasks": ["5.2"] },
    { "id": 5, "tasks": ["5.3", "6.1", "6.2"] }
  ]
}
```
