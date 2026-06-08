# Implementation Plan: MyBiz Dashboard DTO Extension

## Overview

마이 비즈 대시보드 응답 DTO(`MyBizDashboardResponse`)를 4개 탭 구조에 맞게 확장하고, 업종 비교 순위 변동(rankChange) 계산 로직을 추가한다. 기존 API 엔드포인트/호출 방식은 변경하지 않으며, 엔티티에 이미 존재하는 컬럼을 DTO에 새로 매핑하고, 직전 월 데이터 조회를 통한 rankChange 계산을 서비스 레이어에 추가한다.

## Tasks

- [ ] 1. MyBizDashboardResponse DTO 필드 확장
  - [ ] 1.1 MyBizDashboardResponse record에 신규 필드 추가
    - `MyBizDashboardResponse.java`를 설계 문서의 확장 후 구조로 재정의
    - 1번 탭 신규 필드: `prevMonthRevenue`(Long), `monthlyTransactionCount`(Integer), `avgTransactionAmount`(BigDecimal)
    - 3번 탭 신규 필드: `onlineReplyRate`(BigDecimal), `onlineInfoUpdateCount`(Integer), `positiveReviewRatio`(BigDecimal), `deliveryRating`(BigDecimal), `deliverySalesAmount`(Long), `hasOnlineReservation`(Boolean), `hasSns`(Boolean)
    - 필드 순서: 공통 → 1번 탭 → 2번 탭 → 3번 탭 → 4번 탭
    - _Requirements: 1.1, 1.2, 1.3, 1.5_

  - [ ] 1.2 IndustryCompareResponse record에 rankChange 필드 추가
    - `industrySalesRankChange`(BigDecimal, nullable), `industryProfitRankChange`(BigDecimal, nullable), `industryStabilityRankChange`(BigDecimal, nullable) 추가
    - _Requirements: 1.4, 2.1_

- [ ] 2. MyBizConverter 신규 필드 매핑 구현
  - [ ] 2.1 MyBizConverter의 toMyBizDashboardResponse 메서드 확장
    - 메서드 시그니처에 `salesRankChange`, `profitRankChange`, `stabilityRankChange` 파라미터 추가
    - 1번 탭 신규 필드 매핑: `baseData.getPrevMonthRevenue()`, `baseData.getMonthlyTransactionCount()`, `baseData.getAvgTransactionAmount()`
    - 3번 탭 신규 필드 매핑: `baseData.getOnlineReplyRate()`, `baseData.getOnlineInfoUpdateCount()`, `baseData.getPositiveReviewRatio()`, `baseData.getDeliveryRating()`, `baseData.getDeliverySalesAmount()`, `baseData.getHasOnlineReservation()`, `baseData.getHasSns()`
    - IndustryCompareResponse 생성 시 rankChange 파라미터 전달
    - 엔티티 필드 null → DTO 필드 null 전파 보장
    - _Requirements: 3.1, 3.2, 3.3, 5.3, 5.4_

  - [ ]* 2.2 MyBizConverter 단위 테스트 작성
    - 신규 필드 1:1 매핑 정확성 검증
    - rankChange 파라미터 전달 검증
    - 엔티티 필드 null → DTO 필드 null 검증
    - 기존 필드 변환 로직 보존 검증
    - _Requirements: 3.1, 3.2, 3.3, 5.3_

- [ ] 3. MyBizServiceImpl 직전 월 조회 및 rankChange 계산 로직 추가
  - [ ] 3.1 MyBizServiceImpl에 calculateRankChange 메서드 및 직전 월 조회 로직 구현
    - `findDashboard` 메서드에 직전 월(referenceMonth - 1개월) MyBizData 조회 추가 (기존 `findByUser_UserIdAndReferenceMonth` 재사용)
    - `calculateRankChange(BigDecimal currentRank, BigDecimal prevRank)` private 메서드 구현: 둘 다 non-null → 차이값, 그 외 → null
    - 3개 rankChange 값(sales, profit, stability) 계산 후 Converter 호출 시 전달
    - `MyBizConverter.toMyBizDashboardResponse` 호출부 시그니처 변경 반영
    - _Requirements: 2.2, 2.3, 2.4, 4.1, 4.2, 4.3_

  - [ ]* 3.2 MyBizServiceImpl 단위 테스트 작성
    - 직전 월 데이터 존재 시 rankChange 계산 정확성 검증 (Mockito)
    - 직전 월 데이터 미존재 시 rankChange null 검증 (Mockito)
    - 기존 findDashboard 동작 보존 검증
    - _Requirements: 2.2, 2.3, 4.1, 4.2, 4.3_

- [ ] 4. Checkpoint - 컴파일 확인 및 기본 동작 검증
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 5. Property-Based 테스트 작성 (jqwik)
  - [ ]* 5.1 Property 1: calculateRankChange 계산 정확성 테스트
    - **Property 1: rankChange 계산 정확성**
    - 임의의 BigDecimal 쌍에 대해: 둘 다 non-null이면 currentRank - prevRank 반환, 둘 중 하나 null이면 null 반환
    - **Validates: Requirements 2.2, 2.4, 4.2**

  - [ ]* 5.2 Property 2: 직전 월 데이터 부재 시 rankChange null 보장 테스트
    - **Property 2: 직전 월 데이터 부재 시 rankChange null 보장**
    - 직전 월 Optional.empty() 조건에서 IndustryCompareResponse의 3개 rankChange 필드 모두 null이고 나머지 필드는 정상 매핑
    - **Validates: Requirements 1.6, 2.3, 4.3, 5.5**

  - [ ]* 5.3 Property 3: Converter 신규 필드 1:1 매핑 테스트
    - **Property 3: Converter 신규 필드 1:1 매핑**
    - 임의의 MyBizData에 대해 Converter 변환 후 신규 10개 필드가 엔티티 동일 필드와 일치, null 전파 검증
    - **Validates: Requirements 3.1, 3.2, 3.3, 5.4**

  - [ ]* 5.4 Property 4: 기존 필드 값 산출 로직 보존 테스트
    - **Property 4: 기존 필드 값 산출 로직 보존**
    - 임의의 MyBizData와 추이 데이터에 대해 기존 필드(monthlyRevenue, cashFlow, estimatedProfit 등)가 확장 전과 동일한 결과 산출
    - **Validates: Requirements 5.3**

- [ ] 6. Final checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- DB 스키마 변경 없음 — 모든 신규 DTO 필드는 `MyBizData` 엔티티에 이미 존재하는 컬럼 매핑
- Controller/Repository 변경 없음 — 기존 엔드포인트 및 Repository 메서드 재사용
- rankChange는 서비스 레이어 계산값 (current - prev), null 전략으로 데이터 부재 처리
- jqwik Property-Based 테스트는 `build.gradle`에 jqwik 의존성 추가 필요
- 각 property test에 태그 포함: `Feature: mybiz-dashboard-dto-extension, Property {N}: {설명}`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["3.1"] },
    { "id": 3, "tasks": ["2.2", "3.2"] },
    { "id": 4, "tasks": ["5.1", "5.2", "5.3", "5.4"] }
  ]
}
```
