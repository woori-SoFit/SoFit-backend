# Implementation Plan: 대출 신청 상세보기 My Biz Data 탭 조회 API

## Overview

기존 `LoanDashboardController`에 `GET /api/admin/loan-applications/{applicationId}/mybizdata` 엔드포인트를 추가하고, 비즈니스 로직은 새로운 `MyBizDataDetailService`로 분리하여 구현한다. Response DTO는 flat record 구조로 12개 필드(annualIncome, existingLoanCount, monthlyRevenue, monthlyRevenueGrowthRate, cashFlow, accountBalance, businessAgeMonths, vatFilingStatus, taxOverdue, insurancePaymentStatus, industrySalesRank, industryProfitRank)를 포함한다.

## Tasks

- [x] 1. Response DTO 및 SuccessCode 생성
  - [x] 1.1 MyBizDataDetailResponse record 생성
    - `com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse.java` 파일 생성
    - flat record 구조: annualIncome(Long), existingLoanCount(Integer), monthlyRevenue(Long), monthlyRevenueGrowthRate(BigDecimal), cashFlow(Long), accountBalance(Long), businessAgeMonths(Integer), vatFilingStatus(String), taxOverdue(Boolean), insurancePaymentStatus(String), industrySalesRank(BigDecimal), industryProfitRank(BigDecimal)
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

  - [x] 1.2 LoanDashboardSuccessCode에 MY_BIZ_DATA_DETAIL_OK 추가
    - `LoanDashboardSuccessCode.java`에 `MY_BIZ_DATA_DETAIL_OK(HttpStatus.OK, "LOAN2004", "My Biz Data 탭 조회에 성공했습니다.")` 추가
    - _Requirements: 1.3_

- [x] 2. Repository 메서드 추가 및 Converter 생성
  - [x] 2.1 MyBizDataRepository에 최신 데이터 조회 메서드 추가
    - `sofit-common`의 `MyBizDataRepository.java`에 `findFirstByUser_UserIdOrderByReferenceMonthDescBizDataIdDesc(Long userId)` 메서드 추가
    - 반환 타입: `Optional<MyBizData>`
    - reference_month 내림차순 + biz_data_id 내림차순 정렬 (동일 월 결정적 결과 보장)
    - _Requirements: 4.1_

  - [x] 2.2 LoanApplicationRepository에 EXECUTED 카운트 메서드 추가
    - `sofit-common`의 `LoanApplicationRepository.java`에 `countByUser_UserIdAndStatus(Long userId, ApplicationStatus status)` 메서드 추가
    - 반환 타입: `int`
    - 특정 사용자의 EXECUTED 상태 대출 건수 카운트
    - _Requirements: 3.1, 3.2, 3.3_

  - [x] 2.3 MyBizDataDetailConverter 생성
    - `com.sofit.admin.domain.loan.converter.MyBizDataDetailConverter.java` 파일 생성
    - private 생성자 (유틸리티 클래스)
    - `toMyBizDataDetailResponse(MyBizData myBizData, int existingLoanCount)` static 메서드
    - annualIncome, monthlyRevenue, cashFlow, accountBalance, businessAgeMonths, taxOverdue: 엔티티 값 그대로 매핑
    - monthlyRevenueGrowthRate, industrySalesRank, industryProfitRank: BigDecimal 원본 값 변환 없이 그대로 전달
    - vatFilingStatus: null이면 null, 아니면 `.name()` 호출
    - insurancePaymentStatus: null이면 null, 아니면 `.name()` 호출
    - existingLoanCount: 전달받은 int 값 그대로 매핑
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 4.2_

- [x] 3. Service 인터페이스 및 구현체 생성
  - [x] 3.1 MyBizDataDetailService 인터페이스 생성
    - `com.sofit.admin.domain.loan.service.MyBizDataDetailService.java` 파일 생성
    - `MyBizDataDetailResponse findMyBizDataDetail(Long applicationId)` 메서드 선언
    - _Requirements: 1.1_

  - [x] 3.2 MyBizDataDetailServiceImpl 구현체 생성
    - `com.sofit.admin.domain.loan.service.MyBizDataDetailServiceImpl.java` 파일 생성
    - `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)` 어노테이션
    - LoanApplicationRepository, MyBizDataRepository 주입
    - findMyBizDataDetail 구현:
      1. `loanApplicationRepository.findById(applicationId)` → 없으면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      2. `app.getUser().getUserId()`로 userId 획득
      3. `myBizDataRepository.findFirstByUser_UserIdOrderByReferenceMonthDescBizDataIdDesc(userId)` → 없으면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      4. `loanApplicationRepository.countByUser_UserIdAndStatus(userId, ApplicationStatus.EXECUTED)`로 보유 대출 건수 산출
      5. `MyBizDataDetailConverter.toMyBizDataDetailResponse(myBizData, existingLoanCount)` 호출하여 반환
    - _Requirements: 1.1, 3.1, 3.2, 3.3, 4.1, 4.2, 5.1, 5.2_

- [x] 4. Checkpoint - 핵심 로직 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Controller 및 ControllerDocs 확장
  - [x] 5.1 LoanDashboardControllerDocs에 Swagger 메서드 추가
    - 기존 `LoanDashboardControllerDocs.java`에 `findMyBizDataDetail` 메서드 추가
    - `@Operation(summary = "대출 신청 상세 조회 (My Biz Data 탭)")` 어노테이션
    - `@ApiResponses`: 200(My Biz Data 탭 조회 성공), 404(대출 신청 건 또는 My Biz Data를 찾을 수 없음)
    - 파라미터: `@Parameter(description = "대출 신청 ID", example = "1") Long applicationId`
    - 반환 타입: `ApiResponse<MyBizDataDetailResponse>`
    - _Requirements: 1.1, 1.2_

  - [x] 5.2 LoanDashboardController에 엔드포인트 추가
    - 기존 `LoanDashboardController.java`에 `MyBizDataDetailService` 의존성 주입 추가
    - `@GetMapping("/{applicationId}/mybizdata")` 메서드 추가
    - `@Override` + `@PathVariable Long applicationId`
    - Service 호출 후 `ApiResponse.onSuccess(LoanDashboardSuccessCode.MY_BIZ_DATA_DETAIL_OK, response)` 반환
    - _Requirements: 1.1, 1.2, 1.3_

- [x] 6. Checkpoint - 전체 통합 검증
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 7. Property-Based 테스트 작성
  - [ ]* 7.1 Property 1 테스트: Converter 필드 매핑 정확성
    - **Property 1: Converter 필드 매핑 정확성**
    - 임의의 MyBizData(null 가능 필드 포함)와 0 이상의 정수 existingLoanCount에 대해 toMyBizDataDetailResponse 변환 결과가 원본 필드와 일치하는지 검증
    - BigDecimal 필드(monthlyRevenueGrowthRate, industrySalesRank, industryProfitRank)는 원본 값 그대로 전달 확인
    - ENUM 필드(vatFilingStatus, insurancePaymentStatus)는 null이면 null, 아니면 name() 결과와 일치 확인
    - **Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5, 4.2**

- [ ]* 8. 단위 테스트 작성
  - [ ]* 8.1 MyBizDataDetailServiceImpl 단위 테스트
    - 정상 조회 시 12개 필드 포함 응답 반환 검증
    - 존재하지 않는 applicationId → BaseException(NOT_FOUND) 검증
    - MyBizData 미존재 → BaseException(NOT_FOUND) 검증
    - EXECUTED 상태 0건 → existingLoanCount = 0 검증
    - 현재 applicationId가 EXECUTED일 때 카운트에 포함 검증
    - _Requirements: 1.1, 3.1, 3.2, 3.3, 5.1, 5.2_

  - [ ]* 8.2 MyBizDataDetailConverter 단위 테스트
    - 정상 MyBizData → 12개 필드 변환 정확성 검증
    - null 필드 → null 유지 검증
    - BigDecimal 원본 값 그대로 전달 확인
    - ENUM null → vatFilingStatus/insurancePaymentStatus null 반환 검증
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_

- [x] 9. Final Checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 기존 파일(LoanDashboardController, LoanDashboardControllerDocs, LoanDashboardSuccessCode, MyBizDataRepository, LoanApplicationRepository)에는 메서드/필드 추가만 수행
- 인증/권한 검증(Requirements 6.1, 6.2, 6.3)은 기존 Spring Security 필터 체인에서 처리되므로 별도 구현 불필요
- applicationId 형식 오류(Requirements 5.3)는 기존 GlobalExceptionHandler에서 MethodArgumentTypeMismatchException으로 처리됨
- 에러 처리 우선순위(Requirements 5.4): 인증(401) → 권한(403) → 형식 검증(400) → 리소스 존재 여부(404) — 기존 인프라에서 자동 보장
- Property-Based 테스트는 jqwik 라이브러리 사용
- MyBizData 최신 1건 조회 시 reference_month DESC + biz_data_id DESC로 동일 월 결정적 결과 보장

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "2.1", "2.2"] },
    { "id": 1, "tasks": ["2.3", "3.1"] },
    { "id": 2, "tasks": ["3.2"] },
    { "id": 3, "tasks": ["5.1"] },
    { "id": 4, "tasks": ["5.2"] },
    { "id": 5, "tasks": ["7.1"] },
    { "id": 6, "tasks": ["8.1", "8.2"] }
  ]
}
```
