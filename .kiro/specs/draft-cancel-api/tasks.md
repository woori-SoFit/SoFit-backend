# Implementation Plan: DRAFT 대출 신청서 취소 API

## Overview

DRAFT 상태의 대출 신청서를 소프트 삭제(status를 CANCELLED로 변경)하는 DELETE API를 구현한다.
기존 `LoanApplicationController`, `LoanApplicationService`, `LoanApplicationServiceImpl`에 메서드를 추가하고,
`LoanSuccessCode`에 성공 코드를 추가하며, `LoanApplicationControllerDocs`에 Swagger 문서를 추가한다.
기존 에러코드(`APPLICATION_NOT_FOUND`, `APPLICATION_NOT_OWNED`, `APPLICATION_NOT_DRAFT`)는 이미 정의되어 있으므로 재사용한다.

## Tasks

- [ ] 1. 성공 코드 추가 및 Service 인터페이스 메서드 선언
  - [ ] 1.1 LoanSuccessCode에 LOAN_DRAFT_CANCELLED 코드 추가
    - `LoanSuccessCode` enum에 `LOAN_DRAFT_CANCELLED(HttpStatus.OK, "LOAN2017", "신청서가 취소되었습니다.")` 추가
    - 기존 LOAN2016 뒤에 위치
    - _Requirements: 1.2_

  - [ ] 1.2 LoanApplicationService 인터페이스에 cancelDraftApplication 메서드 선언
    - `void cancelDraftApplication(Long userId, Long applicationId);` 메서드 추가
    - _Requirements: 1.1_

- [ ] 2. Service 구현 및 테스트
  - [ ] 2.1 LoanApplicationServiceImpl에 cancelDraftApplication 비즈니스 로직 구현
    - `@Transactional` 어노테이션 적용
    - 검증 순서: 존재 여부 → 본인 소유 → DRAFT 상태
    - 존재하지 않으면 `BaseException(LoanErrorCode.APPLICATION_NOT_FOUND)` throw
    - 본인 소유가 아니면 `BaseException(LoanErrorCode.APPLICATION_NOT_OWNED)` throw
    - DRAFT 상태가 아니면 `BaseException(LoanErrorCode.APPLICATION_NOT_DRAFT)` throw
    - 검증 통과 시 `application.updateStatus(ApplicationStatus.CANCELLED)` 호출
    - _Requirements: 1.1, 1.3, 1.4, 1.5, 1.6, 2.1, 3.1, 3.2, 4.1, 4.2_

  - [ ]* 2.2 cancelDraftApplication 단위 테스트 작성
    - JUnit 5 + Mockito 사용
    - 테스트 클래스: `LoanApplicationServiceImplCancelTest`
    - 성공 케이스: DRAFT 상태 + 본인 소유 → status가 CANCELLED로 변경됨
    - 실패 케이스 1: 존재하지 않는 applicationId → APPLICATION_NOT_FOUND 예외
    - 실패 케이스 2: 타인 소유 → APPLICATION_NOT_OWNED 예외
    - 실패 케이스 3: SUBMITTED 상태 → APPLICATION_NOT_DRAFT 예외
    - 실패 케이스 4: CANCELLED 상태 → APPLICATION_NOT_DRAFT 예외
    - _Requirements: 1.1, 1.4, 1.5, 1.6, 2.1, 3.1, 4.2_

  - [ ]* 2.3 Property 1 테스트 작성: DRAFT 취소 시 소프트 삭제 수행
    - **Property 1: DRAFT 취소 시 소프트 삭제 수행**
    - **Validates: Requirements 1.1, 1.3, 4.1**
    - jqwik 사용, `Feature: draft-cancel-api, Property 1: DRAFT 취소 시 소프트 삭제 수행` 태그
    - 임의의 userId로 본인 소유 DRAFT LoanApplication 생성 → cancelDraftApplication 호출 → status=CANCELLED 확인, 다른 필드는 변경되지 않음 확인

  - [ ]* 2.4 Property 2 테스트 작성: 존재하지 않는 신청에 대한 에러 반환
    - **Property 2: 존재하지 않는 신청에 대한 에러 반환**
    - **Validates: Requirements 1.4, 2.1**
    - jqwik 사용, `Feature: draft-cancel-api, Property 2: 존재하지 않는 신청에 대한 에러 반환` 태그
    - 임의의 applicationId(DB에 존재하지 않는)로 호출 → APPLICATION_NOT_FOUND 예외 발생 확인

  - [ ]* 2.5 Property 3 테스트 작성: 타인 소유 신청 취소 차단 및 상태 보존
    - **Property 3: 타인 소유 신청 취소 차단 및 상태 보존**
    - **Validates: Requirements 1.5, 3.1, 3.2**
    - jqwik 사용, `Feature: draft-cancel-api, Property 3: 타인 소유 신청 취소 차단 및 상태 보존` 태그
    - 소유자와 다른 userId로 호출 → APPLICATION_NOT_OWNED 예외, 원본 status 변경 없음 확인

  - [ ]* 2.6 Property 4 테스트 작성: 비-DRAFT 상태 신청 취소 차단 및 상태 보존
    - **Property 4: 비-DRAFT 상태 신청 취소 차단 및 상태 보존**
    - **Validates: Requirements 1.6, 4.2**
    - jqwik 사용, `Feature: draft-cancel-api, Property 4: 비-DRAFT 상태 신청 취소 차단 및 상태 보존` 태그
    - DRAFT를 제외한 모든 ApplicationStatus에 대해 호출 → APPLICATION_NOT_DRAFT 예외, 원본 status 변경 없음 확인

- [ ] 3. Checkpoint - 중간 검증
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 4. Controller Layer 구현
  - [ ] 4.1 LoanApplicationControllerDocs에 cancelDraftApplication Swagger 문서 추가
    - `@Operation(summary = "DRAFT 신청서 취소")` 정의
    - 200/400/403/404 응답 코드별 `@ApiResponse` 정의
    - `ApiResponse<Void> cancelDraftApplication(@Parameter(description = "대출 신청 ID") Long applicationId);` 선언
    - _Requirements: 1.2_

  - [ ] 4.2 LoanApplicationController에 DELETE 엔드포인트 추가
    - `@DeleteMapping("/loan-applications/{applicationId}")` 매핑
    - `SecurityUtil.getCurrentUserId()`로 userId 추출
    - `loanApplicationService.cancelDraftApplication(userId, applicationId)` 호출
    - `ApiResponse.onSuccess(LoanSuccessCode.LOAN_DRAFT_CANCELLED, null)` 반환
    - _Requirements: 1.1, 1.2_

  - [ ]* 4.3 Controller 통합 테스트 작성
    - `@SpringBootTest` + `@AutoConfigureMockMvc` 사용
    - 성공 케이스: DRAFT 신청 취소 → 200 + LOAN2017 코드 확인
    - 에러 케이스: 404, 403, 400 응답 확인
    - 취소 후 동일 상품 재신청 가능 여부 확인 (Property 5 검증)
    - _Requirements: 1.1, 1.2, 2.1, 3.1, 4.2, 5.1, 5.2_

- [ ] 5. Final checkpoint - 최종 검증
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 기존 에러코드(`APPLICATION_NOT_FOUND`, `APPLICATION_NOT_OWNED`, `APPLICATION_NOT_DRAFT`)는 `LoanErrorCode`에 이미 정의되어 있으므로 새로 추가하지 않음
- `LoanApplication.updateStatus()` 메서드가 이미 존재하므로 엔티티 변경 불필요
- 기존 `checkDraft()`, `createApplication()`, `getResumeData()` 로직은 CANCELLED 상태를 이미 정상적으로 제외하므로 조회 로직 수정 불필요
- Property 5(CANCELLED 후 재신청 가능)는 기존 로직에서 이미 지원하므로 통합 테스트에서만 검증
- jqwik 의존성이 프로젝트에 없을 경우 `build.gradle`에 추가 필요: `testImplementation 'net.jqwik:jqwik:1.8.2'`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "2.3", "2.4", "2.5", "2.6", "4.1"] },
    { "id": 3, "tasks": ["4.2"] },
    { "id": 4, "tasks": ["4.3"] }
  ]
}
```
