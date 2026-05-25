# Implementation Plan: MyBiz Dashboard API

## Overview

소상공인 사용자의 My Biz Data 대시보드 조회 API(`GET /api/my-biz/dashboard`)를 구현한다. 기존 SoFit 프로젝트의 레이어드 아키텍처와 컨벤션을 따르며, sofit-common 모듈에 Entity/Repository를, sofit-user 모듈에 Controller/Service/Converter/DTO를 배치한다.

## Git 정보

- **브랜치**: `feat/SOFIT-41-mybiz-dashboard`
- **커밋 형식**: `[SOFIT-41] Feat: {작업 내용}`

---

## Tasks

- [x] 1. Entity 및 Repository 생성 (sofit-common)
  - [x] 1.1 MyBizData Entity 생성
    - `sofit-common/src/main/java/com/sofit/common/entity/mybiz/MyBizData.java` 생성
    - BaseEntity 상속, my_biz_data 테이블 매핑
    - User @ManyToOne 연관관계, referenceMonth(LocalDate), 매출/현금흐름/업종비교 필드 정의
    - _Requirements: 1.2, 1.4, 1.5_

  - [x] 1.2 MyBizDataRepository 생성
    - `sofit-common/src/main/java/com/sofit/common/repository/MyBizDataRepository.java` 생성
    - findFirstByUser_UserIdOrderByReferenceMonthDesc (최신 데이터 조회)
    - findByUser_UserIdAndReferenceMonth (특정 월 조회)
    - findByUser_UserIdAndReferenceMonthBetweenOrderByReferenceMonthAsc (범위 조회)
    - _Requirements: 2.1, 2.2, 3.1, 4.1_

- [x] 2. DTO 및 Enum 생성 (sofit-user)
  - [x] 2.1 MyBizDashboardResponse DTO 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/mybiz/dto/response/MyBizDashboardResponse.java` 생성
    - record 타입으로 정의
    - 내부 record: IndustryCompareResponse, RevenueTrendResponse, CashFlowTrendResponse
    - _Requirements: 1.2, 1.3, 1.4, 1.5, 3.3, 4.3_

  - [x] 2.2 MyBizErrorCode Enum 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/mybiz/exception/MyBizErrorCode.java` 생성
    - MY_BIZ_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "MYBIZ4041", "My Biz Data가 존재하지 않습니다.")
    - BaseErrorCode 인터페이스 구현
    - _Requirements: 5.1, 5.2_

  - [x] 2.3 MyBizSuccessCode Enum 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/mybiz/enums/MyBizSuccessCode.java` 생성
    - DASHBOARD_OK(HttpStatus.OK, "MYBIZ2001", "성공입니다.")
    - BaseSuccessCode 인터페이스 구현
    - _Requirements: 1.1_

- [x] 3. Converter 및 Service 구현 (sofit-user)
  - [x] 3.1 MyBizConverter 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/mybiz/converter/MyBizConverter.java` 생성
    - private 생성자 (유틸리티 클래스)
    - static 메서드: toMyBizDashboardResponse(MyBizData baseData, List<MyBizData> revenueTrendData, List<MyBizData> cashFlowTrendData)
    - referenceMonth를 yyyy-MM 형식 문자열로 변환
    - _Requirements: 1.2, 1.3, 1.4, 3.3, 4.3_

  - [x] 3.2 MyBizService 인터페이스 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/mybiz/service/MyBizService.java` 생성
    - MyBizDashboardResponse findDashboard(Long userId, String month)
    - _Requirements: 1.1, 2.1, 2.2_

  - [x] 3.3 MyBizServiceImpl 구현
    - `sofit-user/src/main/java/com/sofit/user/domain/mybiz/service/MyBizServiceImpl.java` 생성
    - month 파라미터 검증: null/빈 문자열 → 최신, yyyy-MM 정규식 매칭 → 파싱, 그 외 → BAD_REQUEST
    - 기준월 데이터 조회 (미존재 시 MY_BIZ_DATA_NOT_FOUND 예외)
    - revenueTrend 조회 (기준월 포함 이전 5개월, 오름차순)
    - cashFlowTrend 조회 (기준월 포함 이전 3개월, 오름차순)
    - Converter로 DTO 변환 후 반환
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.4, 3.5, 4.1, 4.2, 4.4, 4.5, 5.1, 5.2_

- [x] 4. Checkpoint - 컴파일 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 5. Controller 및 Swagger 문서 구현 (sofit-user)
  - [x] 5.1 MyBizControllerDocs 인터페이스 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/mybiz/controller/MyBizControllerDocs.java` 생성
    - @Tag(name = "My Biz Data", description = "소상공인 My Biz Data 대시보드 조회 API")
    - @Operation, @ApiResponses(200, 401, 404), @Parameter(month, required=false, example="2024-05")
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [x] 5.2 MyBizController 생성
    - `sofit-user/src/main/java/com/sofit/user/domain/mybiz/controller/MyBizController.java` 생성
    - @RestController, @RequestMapping("/api/my-biz")
    - implements MyBizControllerDocs
    - @GetMapping("/dashboard") findDashboard 메서드
    - extractUserId(session) 패턴으로 세션에서 userId 추출
    - ApiResponse.onSuccess(MyBizSuccessCode.DASHBOARD_OK, response) 반환
    - _Requirements: 1.1, 6.1, 6.2_

- [x] 6. Checkpoint - 전체 컴파일 및 동작 확인
  - Ensure all tests pass, ask the user if questions arise.

- [x] 7. 단위 테스트 작성
  - [x] 7.1 MyBizServiceImpl 단위 테스트
    - `sofit-user/src/test/java/com/sofit/user/domain/mybiz/service/MyBizServiceImplTest.java` 생성
    - month=null → 최신 데이터 반환 검증
    - month="2024-05" → 해당 월 데이터 반환 검증
    - month="" → 최신 데이터 반환 검증 (빈 문자열)
    - 데이터 미존재 → MY_BIZ_DATA_NOT_FOUND 예외 검증
    - 잘못된 month 형식 → BAD_REQUEST 예외 검증
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 5.1, 5.2_

  - [x] 7.2 MyBizConverter 단위 테스트
    - `sofit-user/src/test/java/com/sofit/user/domain/mybiz/converter/MyBizConverterTest.java` 생성
    - Entity → DTO 변환 정확성 검증
    - referenceMonth yyyy-MM 형식 변환 검증
    - revenueTrend, cashFlowTrend 리스트 변환 검증
    - _Requirements: 1.2, 1.3, 3.3, 4.3_

- [x] 8. 프로퍼티 기반 테스트 작성 (jqwik)
  - [x] 8.1 Property 1: Converter 변환 완전성 테스트
    - **Property 1: Converter 변환 완전성 (Round-trip field preservation)**
    - 랜덤 MyBizData 생성 → Converter 변환 → 필수 필드 non-null + yyyy-MM 형식 확인
    - **Validates: Requirements 1.2, 1.3, 1.4, 3.3, 4.3**

  - [x] 8.2 Property 2: 기본 조회 시 최신 데이터 선택 테스트
    - **Property 2: 기본 조회 시 최신 데이터 선택**
    - 랜덤 데이터 목록 생성 → month=null 조회 → MAX referenceMonth 일치 확인
    - **Validates: Requirements 2.1**

  - [x] 8.3 Property 3: 잘못된 month 형식 거부 테스트
    - **Property 3: 잘못된 month 형식 거부**
    - yyyy-MM 정규식에 매칭되지 않는 문자열 생성 → 검증 로직 → 항상 거부 확인
    - **Validates: Requirements 2.3**

  - [x] 8.4 Property 4: revenueTrend 범위 및 정렬 불변식 테스트
    - **Property 4: revenueTrend 범위 및 정렬 불변식**
    - 랜덤 데이터 목록 + 기준월 → revenueTrend 크기(0~5)/범위/오름차순 정렬 확인
    - **Validates: Requirements 3.1, 3.2, 3.4, 3.5**

  - [x] 8.5 Property 5: cashFlowTrend 범위 및 정렬 불변식 테스트
    - **Property 5: cashFlowTrend 범위 및 정렬 불변식**
    - 랜덤 데이터 목록 + 기준월 → cashFlowTrend 크기(0~3)/범위/오름차순 정렬 확인
    - **Validates: Requirements 4.1, 4.2, 4.4, 4.5**

- [x] 9. Final Checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 Task는 특정 Requirements를 참조하여 추적 가능
- sofit-common 모듈: Entity(`entity/mybiz/`), Repository(`repository/`)
- sofit-user 모듈: 나머지 모든 클래스(`domain/mybiz/` 하위)
- mybiz 도메인 디렉토리 구조는 이미 .gitkeep으로 생성되어 있음
- 인증은 기존 세션 필터가 처리하므로 별도 구현 불필요
- Property 테스트는 jqwik 라이브러리 사용 (build.gradle에 의존성 추가 필요)

## 커밋 가이드

| Phase | 커밋 메시지 |
|-------|------------|
| Phase 1 (Task 1) | `[SOFIT-41] Feat: MyBizData Entity 및 Repository 생성` |
| Phase 2 (Task 2) | `[SOFIT-41] Feat: MyBiz DTO 및 Enum 생성` |
| Phase 3 (Task 3) | `[SOFIT-41] Feat: MyBiz Converter 및 Service 구현` |
| Phase 4 (Task 5) | `[SOFIT-41] Feat: MyBiz Controller 및 Swagger 문서 구현` |
| Phase 5 (Task 7~8) | `[SOFIT-41] Test: MyBiz 단위 테스트 및 프로퍼티 테스트 작성` |

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["2.1", "2.2", "2.3"] },
    { "id": 2, "tasks": ["3.1", "3.2"] },
    { "id": 3, "tasks": ["3.3"] },
    { "id": 4, "tasks": ["5.1"] },
    { "id": 5, "tasks": ["5.2"] },
    { "id": 6, "tasks": ["7.1", "7.2"] },
    { "id": 7, "tasks": ["8.1", "8.2", "8.3", "8.4", "8.5"] }
  ]
}
```
