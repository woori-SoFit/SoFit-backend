# Implementation Plan: 대출 신청 상세보기 성장 S등급 탭 조회 API

## Overview

기존 `LoanDashboardController`에 `GET /api/admin/loan-applications/{applicationId}/grade` 엔드포인트를 추가하고, 비즈니스 로직은 새로운 `LoanApplicationGradeService`로 분리하여 구현한다. Scb, SScoringRule 신규 엔티티와 Repository를 생성하고, ShapExplanation의 SHAP Details를 "피처명:SHAP점수" 형식에서 Map<String, Double>로 파싱하는 Converter를 구현한다. Response DTO는 중첩 record 구조(cbScore, sGrade, scbInfo, shapResult)로 구성한다.

## Tasks

- [x] 1. 신규 엔티티 및 Repository 생성
  - [x] 1.1 Scb 엔티티 생성
    - `sofit-common/src/main/java/com/sofit/common/entity/report/Scb.java` 파일 생성
    - `@Entity`, `@Table(name = "scb")`, `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
    - 필드: scbId(PK, IDENTITY), user(ManyToOne LAZY, user_id), applicationId(Long), cbGrade(Integer), sGrade(Integer), scoreAddition(Integer), scbGrade(Integer)
    - BaseEntity 상속
    - _Requirements: 2.1, 4.1_

  - [x] 1.2 SScoringRule 엔티티 생성
    - `sofit-common/src/main/java/com/sofit/common/entity/report/SScoringRule.java` 파일 생성
    - `@Entity`, `@Table(name = "s_scoring_rule")`, `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)`
    - 필드: grade(String, PK), scoreAddition(Integer), description(String)
    - BaseEntity 상속하지 않음 (독립 엔티티)
    - _Requirements: 4.2_

  - [x] 1.3 ScbRepository 생성
    - `sofit-common/src/main/java/com/sofit/common/repository/ScbRepository.java` 파일 생성
    - `JpaRepository<Scb, Long>` 상속
    - `Optional<Scb> findByApplicationId(Long applicationId)` 메서드 선언
    - _Requirements: 1.1, 2.1_

  - [x] 1.4 SScoringRuleRepository 생성
    - `sofit-common/src/main/java/com/sofit/common/repository/SScoringRuleRepository.java` 파일 생성
    - `JpaRepository<SScoringRule, String>` 상속
    - 기본 `findById(String grade)` 사용
    - _Requirements: 4.2_

- [x] 2. Response DTO 및 SuccessCode 생성
  - [x] 2.1 LoanApplicationGradeResponse record 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/dto/response/LoanApplicationGradeResponse.java` 파일 생성
    - 최상위 record: cbScore(CbScoreInfo), sGrade(String), scbInfo(ScbInfo), shapResult(ShapResult)
    - 중첩 record CbScoreInfo: score(Integer), maxScore(Integer)
    - 중첩 record ScbInfo: score(Integer), maxScore(Integer), bonusPoints(Integer)
    - 중첩 record ShapResult: grade(String), targetGrade(String), strengthKeywords(List<String>), improvementKeywords(List<String>), strengthDetails(Map<String, Double>), improvementDetails(Map<String, Double>), advice(String)
    - _Requirements: 1.1, 2.2, 2.3, 3.1, 4.1, 5.2, 5.3, 5.4, 5.5_

  - [x] 2.2 LoanDashboardSuccessCode에 LOAN_APPLICATION_GRADE_OK 추가
    - `LoanDashboardSuccessCode.java`에 `LOAN_APPLICATION_GRADE_OK(HttpStatus.OK, "LOAN2005", "성장 S등급 탭 조회에 성공했습니다.")` 추가
    - _Requirements: 1.3_

- [x] 3. Converter 생성
  - [x] 3.1 LoanApplicationGradeConverter 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/converter/LoanApplicationGradeConverter.java` 파일 생성
    - private 생성자 (유틸리티 클래스)
    - `toLoanApplicationGradeResponse(Scb scb, SGrade sGrade, SScoringRule scoringRule, ShapExplanation shapExplanation)` static 메서드
    - cbScore 섹션: score = scb.getCbGrade(), maxScore = 1000
    - sGrade: sGrade.getLabel()
    - scbInfo 섹션: score = scb.getScbGrade(), maxScore = 1000, bonusPoints = scoringRule.getScoreAddition()
    - shapResult 섹션: grade = shapExplanation.getSGrade().getLabel(), targetGrade = null이면 null 아니면 getLabel(), strengthKeywords/improvementKeywords = null이면 빈 리스트, strengthDetails/improvementDetails = parseShapDetails() 호출, advice = 그대로
    - `parseShapDetails(List<String> details)` public static 메서드: 콜론(:)으로 분리, LinkedHashMap 사용, 파싱 실패 원소 건너뜀, 원본 값 그대로 전달
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 4.1, 4.3, 4.4, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 4. Service 인터페이스 및 구현체 생성
  - [x] 4.1 LoanApplicationGradeService 인터페이스 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/LoanApplicationGradeService.java` 파일 생성
    - `LoanApplicationGradeResponse findLoanApplicationGrade(Long applicationId)` 메서드 선언
    - _Requirements: 1.1_

  - [x] 4.2 LoanApplicationGradeServiceImpl 구현체 생성
    - `sofit-admin/src/main/java/com/sofit/admin/domain/loan/service/LoanApplicationGradeServiceImpl.java` 파일 생성
    - `@Service`, `@RequiredArgsConstructor`, `@Transactional(readOnly = true)` 어노테이션
    - LoanApplicationRepository, ScbRepository, SScoringRuleRepository, ShapExplanationRepository 주입
    - findLoanApplicationGrade 구현:
      1. `loanApplicationRepository.findById(applicationId)` → 없으면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      2. `scbRepository.findByApplicationId(applicationId)` → 없으면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      3. `convertToSGrade(scb.getSGrade())` → 범위 밖이면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      4. `sScoringRuleRepository.findById(sGrade.getLabel())` → 없으면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      5. `app.getSEvaluationId()` null 체크 → null이면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      6. `shapExplanationRepository.findById(sEvaluationId)` → 없으면 `BaseException(GeneralErrorCode.NOT_FOUND)`
      7. `LoanApplicationGradeConverter.toLoanApplicationGradeResponse(scb, sGrade, scoringRule, shapExplanation)` 호출하여 반환
    - private `convertToSGrade(Integer sGradeValue)` 메서드: null 또는 1~10 범위 밖이면 예외, SGrade.values()[sGradeValue - 1] 반환
    - _Requirements: 1.1, 2.1, 3.1, 3.2, 4.1, 4.2, 5.1, 6.2, 6.3, 6.4, 6.5_

- [x] 5. Checkpoint - 핵심 로직 검증
  - Ensure all tests pass, ask the user if questions arise.

- [x] 6. Controller 및 ControllerDocs 확장
  - [x] 6.1 LoanDashboardControllerDocs에 Swagger 메서드 추가
    - 기존 `LoanDashboardControllerDocs.java`에 `findLoanApplicationGrade` 메서드 추가
    - `@Operation(summary = "대출 신청 상세 조회 (성장 S등급 탭)")` 어노테이션
    - `@ApiResponses`: 200(성장 S등급 탭 조회 성공), 404(대출 신청 건, SCB 정보 또는 SHAP 분석 결과를 찾을 수 없음)
    - 파라미터: `@Parameter(description = "대출 신청 ID", example = "1") Long applicationId`
    - 반환 타입: `ApiResponse<LoanApplicationGradeResponse>`
    - _Requirements: 1.1, 1.2_

  - [x] 6.2 LoanDashboardController에 엔드포인트 추가
    - 기존 `LoanDashboardController.java`에 `LoanApplicationGradeService` 의존성 주입 추가
    - `@GetMapping("/{applicationId}/grade")` 메서드 추가
    - `@Override` + `@PathVariable Long applicationId`
    - Service 호출 후 `ApiResponse.onSuccess(LoanDashboardSuccessCode.LOAN_APPLICATION_GRADE_OK, response)` 반환
    - _Requirements: 1.1, 1.2, 1.3_

- [x] 7. Checkpoint - 전체 통합 검증
  - Ensure all tests pass, ask the user if questions arise.

- [ ]* 8. Property-Based 테스트 작성
  - [ ]* 8.1 Property 1 테스트: Converter 전체 필드 매핑 정확성
    - **Property 1: Converter 전체 필드 매핑 정확성**
    - 임의의 유효한 Scb(cbGrade: 0~1000 또는 null, sGrade: 1~10, scbGrade: 0~1000), SGrade enum, SScoringRule(scoreAddition: 0 이상), ShapExplanation에 대해 toLoanApplicationGradeResponse 변환 결과가 원본 필드와 일치하는지 검증
    - cbScore.score == scb.cbGrade, cbScore.maxScore == 1000, sGrade == SGrade.label, scbInfo.score == scb.scbGrade, scbInfo.bonusPoints == scoringRule.scoreAddition
    - shapResult.grade == shapExplanation.sGrade.getLabel(), targetGrade null 처리, keywords null → 빈 리스트
    - **Validates: Requirements 2.1, 2.2, 2.3, 3.1, 4.1, 4.2, 4.3, 4.4, 5.2, 5.3, 5.5, 5.6**

  - [ ]* 8.2 Property 2 테스트: SHAP Details 파싱 라운드트립
    - **Property 2: SHAP Details 파싱 라운드트립**
    - 임의의 유효한 "피처명:SHAP점수" 형식 문자열 리스트에 대해 parseShapDetails 결과의 크기, 키 순서, 각 키/값이 원본과 일치하는지 검증
    - LinkedHashMap 순서 유지 확인, 원본 Double 값 그대로 전달 확인
    - **Validates: Requirements 5.4, 8.1, 8.2, 8.3, 8.4**

  - [ ]* 8.3 Property 3 테스트: SHAP Details 파싱 내결함성
    - **Property 3: SHAP Details 파싱 내결함성**
    - 유효/무효 원소 혼합 리스트에 대해 parseShapDetails 결과에 유효 원소만 포함, 무효 원소 제외, 유효 원소 상대적 순서 유지 검증
    - **Validates: Requirements 5.8, 8.5**

- [ ]* 9. 단위 테스트 작성
  - [ ]* 9.1 LoanApplicationGradeServiceImpl 단위 테스트
    - 정상 조회 시 4개 섹션(cbScore, sGrade, scbInfo, shapResult) 포함 응답 반환 검증
    - 존재하지 않는 applicationId → BaseException(NOT_FOUND) 검증
    - Scb 미존재 → BaseException(NOT_FOUND) 검증
    - s_grade 범위 밖(0, 11, null) → BaseException(NOT_FOUND) 검증
    - s_evaluation_id null → BaseException(NOT_FOUND) 검증
    - ShapExplanation 미존재 → BaseException(NOT_FOUND) 검증
    - _Requirements: 1.1, 3.2, 6.2, 6.3, 6.4, 6.5_

  - [ ]* 9.2 LoanApplicationGradeConverter 단위 테스트
    - 정상 데이터 → 전체 필드 변환 정확성 검증
    - cbGrade null → cbScore.score null, maxScore 1000 유지 검증
    - targetGrade null → shapResult.targetGrade null 검증
    - keywords null → 빈 배열 반환 검증
    - parseShapDetails 정상 파싱 + 원본 값 그대로 전달 확인
    - parseShapDetails 무효 원소 건너뛰기 검증
    - parseShapDetails LinkedHashMap 순서 유지 검증
    - parseShapDetails null/빈 리스트 → 빈 Map 반환 검증
    - _Requirements: 2.1, 2.2, 2.3, 4.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.7, 5.8, 8.1, 8.2, 8.3, 8.4, 8.5_

- [x] 10. Final Checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 기존 파일(LoanDashboardController, LoanDashboardControllerDocs, LoanDashboardSuccessCode)에는 메서드/필드 추가만 수행
- 인증/권한 검증(Requirements 7.1, 7.2, 7.3, 7.4)은 기존 Spring Security 필터 체인(SessionAuthFilter)에서 처리되므로 별도 구현 불필요
- applicationId 형식 오류(Requirements 6.1)는 기존 GlobalExceptionHandler에서 MethodArgumentTypeMismatchException으로 처리됨
- 에러 처리 우선순위(Requirements 6.5): 인증(401) → 권한(403) → 형식 검증(400) → LoanApplication 존재(404) → Scb 존재(404) → s_grade 유효성(404) → ShapExplanation 존재(404) — 기존 인프라에서 자동 보장
- Property-Based 테스트는 jqwik 라이브러리 사용
- SHAP 점수는 반올림 없이 원본 데이터 그대로 전달
- SHAP Details 파싱 시 LinkedHashMap으로 원본 순서 유지

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3", "1.4", "2.1", "2.2"] },
    { "id": 1, "tasks": ["3.1", "4.1"] },
    { "id": 2, "tasks": ["4.2"] },
    { "id": 3, "tasks": ["6.1"] },
    { "id": 4, "tasks": ["6.2"] },
    { "id": 5, "tasks": ["8.1", "8.2", "8.3"] },
    { "id": 6, "tasks": ["9.1", "9.2"] }
  ]
}
```
