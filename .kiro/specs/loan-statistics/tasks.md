# Implementation Plan: 대출 현황 통계 API

## Overview

기존 `LoanDashboardController`에 `GET /api/admin/loan-applications/statistics` 엔드포인트를 추가하여, 대출 신청 건의 상태별 통계(pending, managerReview, approved, rejected)를 반환하는 읽기 전용 집계 API를 구현한다. 단일 JPQL GROUP BY 쿼리로 DB 왕복을 최소화하고, 별도의 `LoanStatisticsService`로 책임을 분리한다.

## Tasks

- [x] 1. Repository 및 Projection 생성
  - [x] 1.1 StatusCountProjection 인터페이스 생성
    - `sofit-common/src/main/java/com/sofit/common/repository/projection/StatusCountProjection.java` 파일 생성
    - `getStatus()` → ApplicationStatus, `getCount()` → Long 메서드 정의
    - _Requirements: 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 LoanApplicationRepository에 통계 쿼리 메서드 추가
    - `sofit-common/src/main/java/com/sofit/common/repository/LoanApplicationRepository.java`에 `countByStatuses` 메서드 추가
    - JPQL: `SELECT la.status AS status, COUNT(la) AS count FROM LoanApplication la WHERE la.status IN :statuses GROUP BY la.status`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

- [x] 2. DTO 및 Converter 생성
  - [x] 2.1 LoanStatisticsResponse record 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/dto/response/LoanStatisticsResponse.java` 파일 생성
    - 필드: `int pending`, `int managerReview`, `int approved`, `int rejected`
    - _Requirements: 4.2, 4.4_

  - [x] 2.2 LoanStatisticsConverter 클래스 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/converter/LoanStatisticsConverter.java` 파일 생성
    - `toLoanStatisticsResponse(List<StatusCountProjection> counts)` static 메서드 구현
    - StatusCountProjection 목록을 Map으로 변환 후 pending(SYSTEM_APPROVED + SYSTEM_REJECTED), managerReview, approved, rejected 산출
    - 해당 상태가 없으면 0으로 처리
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.7, 4.4_

- [x] 3. Service 레이어 구현
  - [x] 3.1 LoanStatisticsService 인터페이스 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/LoanStatisticsService.java` 파일 생성
    - `LoanStatisticsResponse getStatistics()` 메서드 선언
    - _Requirements: 1.1_

  - [x] 3.2 LoanStatisticsServiceImpl 구현체 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/LoanStatisticsServiceImpl.java` 파일 생성
    - `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)` 적용
    - STATISTICS_STATUSES 상수 정의: `SYSTEM_APPROVED, SYSTEM_REJECTED, MANAGER_REVIEW, APPROVED, REJECTED`
    - Repository에서 countByStatuses 호출 후 Converter로 변환하여 반환
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.7, 3.2_

- [x] 4. Controller 및 Swagger 문서 연동
  - [x] 4.1 LoanDashboardSuccessCode에 LOAN_STATISTICS_OK 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/exception/LoanDashboardSuccessCode.java`에 enum 값 추가
    - `LOAN_STATISTICS_OK(HttpStatus.OK, "COMMON2000", "성공입니다.")`
    - _Requirements: 1.6, 4.1_

  - [x] 4.2 LoanDashboardControllerDocs에 getStatistics 메서드 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/controller/LoanDashboardControllerDocs.java`에 Swagger 문서 추가
    - `@Operation(summary = "대출 현황 통계 조회", description = "상태별 대출 신청 건수 통계를 조회합니다.")`
    - _Requirements: 4.1, 4.2, 4.3_

  - [x] 4.3 LoanDashboardController에 getStatistics 엔드포인트 추가
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/controller/LoanDashboardController.java`에 메서드 추가
    - `@GetMapping("/statistics")` 매핑
    - LoanStatisticsService 주입 및 호출
    - `ApiResponse.onSuccess(LoanDashboardSuccessCode.LOAN_STATISTICS_OK, response)` 반환
    - _Requirements: 1.1, 1.6, 2.1, 2.2, 4.1_

- [x] 5. Checkpoint - 컴파일 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. 단위 테스트 작성
  - [x]* 6.1 LoanStatisticsConverter 단위 테스트 작성
    - `sofit-admin/src/test/java/com/sofit/admin/domain/loan/converter/LoanStatisticsConverterTest.java` 생성
    - 정상 케이스: 모든 상태 존재 시 올바른 집계 확인
    - 빈 목록 입력 시 모든 필드 0 반환 확인
    - 일부 상태만 존재하는 시나리오 확인
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 1.7_

  - [x]* 6.2 LoanStatisticsServiceImpl 단위 테스트 작성
    - `sofit-admin/src/test/java/com/sofit/admin/domain/loan/service/LoanStatisticsServiceImplTest.java` 생성
    - Mockito로 Repository Mock 사용
    - 다양한 상태 분포에 대한 집계 결과 검증
    - 빈 결과(모든 상태 0건) 시나리오
    - _Requirements: 1.1, 1.7, 3.2_

  - [x]* 6.3 Property-Based Test: 통계 집계 정확성 (Property 1)
    - `sofit-admin/src/test/java/com/sofit/admin/domain/loan/converter/LoanStatisticsConverterPropertyTest.java` 생성
    - jqwik 라이브러리 사용
    - 임의의 ApplicationStatus 분포를 가진 StatusCountProjection 목록 생성
    - Converter 출력이 기대 집계 값과 일치하는지 검증
    - **Property 1: 통계 집계 정확성**
    - **Validates: Requirements 1.2, 1.3, 1.4, 1.5, 1.7, 4.2, 4.4**

- [x] 7. Final checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 태스크는 특정 요구사항을 참조하여 추적 가능성을 보장합니다
- Checkpoint에서 점진적 검증을 수행합니다
- Property 테스트는 jqwik 라이브러리를 사용하여 Converter의 집계 정확성을 검증합니다
- 인증/권한 검증(Requirement 2)은 기존 Spring Security 설정에 의해 자동 처리되므로 별도 구현 태스크 불필요
- 서버 오류 처리(Requirement 3)는 기존 GlobalExceptionHandler에 의해 자동 처리됨

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["1.2", "2.2"] },
    { "id": 2, "tasks": ["3.1", "4.1"] },
    { "id": 3, "tasks": ["3.2", "4.2"] },
    { "id": 4, "tasks": ["4.3"] },
    { "id": 5, "tasks": ["6.1", "6.2", "6.3"] }
  ]
}
```
