# Requirements Document

## Introduction

은행원(BANK_ADMIN)이 대출 신청 상세보기 화면의 성장 S등급 탭에서 CB 신용점수, SCB 점수, 성장 S등급, SHAP 기반 분석 결과(강점/개선점 키워드, 피처별 SHAP 점수, AI 개선 조언)를 조회할 수 있는 API를 제공한다. 이 API는 `GET /api/admin/loan-applications/{applicationId}/grade` 엔드포인트로 제공되며, 대출 심사 시 은행원이 신청자의 성장 S등급 산출 근거를 상세히 파악하는 데 사용된다.

## Glossary

- **Loan_Application_Grade_API**: 대출 신청 상세보기 성장 S등급 탭 조회 API. applicationId를 Path Variable로 받아 해당 신청 건의 CB 점수, SCB 점수, S등급, SHAP 분석 결과를 반환하는 GET 엔드포인트
- **LoanApplication**: 대출 신청 엔티티. loan_application 테이블에 매핑되며 application_id(PK), user_id(FK), s_evaluation_id 필드를 포함
- **Scb**: SCB 엔티티. scb 테이블에 매핑되며 scb_id(PK), user_id(FK), application_id(FK), cb_grade(CB 점수), s_grade(S등급 점수), score_addition(가산점), scb_grade(SCB 점수) 필드를 포함
- **ShapExplanation**: SHAP 설명 엔티티. shap_explanation 테이블에 매핑되며 evaluation_id(PK), user_id(FK), s_grade, target_grade, strength_keywords, improvement_keywords, strength_details, improvement_details, advice 필드를 포함
- **SGrade**: 성장 S등급 열거형. S1~S10 값을 가지며 소상공인의 성장 가능성을 나타내는 등급
- **CB_Score**: CB(Credit Bureau) 신용점수. 외부 신용평가기관에서 산출한 점수
- **SCB_Score**: CB 점수에 성장 S등급 가산점을 합산한 최종 점수
- **SHAP_Score**: 각 피처가 S등급 산출에 기여한 정도를 나타내는 수치. 양수는 등급 상승 기여(강점), 음수는 등급 하락 기여(개선점)
- **SScoringRule**: S등급 가산점 규칙 엔티티. s_scoring_rule 테이블에 매핑되며 grade(성장 등급 S1~S10, PK), score_addition(가산점), description(등급별 성장성 상태 설명) 필드를 포함. Scb의 s_grade에 해당하는 등급의 가산점과 설명을 제공
- **ApiResponse**: 공통 응답 포맷. isSuccess, code, message, result 필드로 구성

## Requirements

### Requirement 1: 성장 S등급 탭 조회

**User Story:** 은행원으로서, 대출 신청 건의 성장 S등급 탭을 조회하여, CB 점수·SCB 점수·S등급·SHAP 분석 결과를 한 화면에서 확인하고 싶다.

#### Acceptance Criteria

1. WHEN applicationId를 Path Variable로 포함한 GET /api/admin/loan-applications/{applicationId}/grade 요청이 수신되면, THE Loan_Application_Grade_API SHALL LoanApplication(application_id로 조회) → Scb(application_id로 조회) → ShapExplanation(LoanApplication의 s_evaluation_id = evaluation_id로 조회) 순서로 엔티티를 조회하여 result 필드 내에 cbScore, sGrade, scbInfo, shapResult 4개 섹션을 포함하는 응답을 반환한다
2. THE Loan_Application_Grade_API SHALL 응답을 ApiResponse 공통 포맷(isSuccess, code, message, result)으로 래핑하여 반환한다
3. WHEN 조회가 성공하면, THE Loan_Application_Grade_API SHALL HTTP 200 상태 코드와 함께 isSuccess: true, code "COMMON2000", message "성공입니다."를 반환한다
4. THE Loan_Application_Grade_API SHALL API 응답 시간을 3초 이내로 유지한다

### Requirement 2: CB 신용점수(cbScore) 조회

**User Story:** 은행원으로서, 신청자의 CB 신용점수를 확인하여, 기본 신용도를 파악하고 싶다.

#### Acceptance Criteria

1. WHEN LoanApplication이 조회되면, THE Loan_Application_Grade_API SHALL application_id로 Scb 엔티티를 조회하여 cbScore 섹션을 구성한다
2. THE Loan_Application_Grade_API SHALL cbScore에 score(CB 신용점수, Scb의 cb_grade 값, 0 이상 1000 이하의 정수) 필드와 maxScore(CB 최대 점수, 1000 고정값, 정수) 필드를 포함한다
3. IF Scb 엔티티는 존재하나 cb_grade 값이 null이면, THEN THE Loan_Application_Grade_API SHALL score 필드를 null로 반환하고 maxScore 필드는 1000을 유지한다

### Requirement 3: 최종 SCB 등급(sGrade) 조회

**User Story:** 은행원으로서, 신청자의 최종 SCB 등급을 확인하여, 성장 S등급 기반 종합 신용등급을 파악하고 싶다.

#### Acceptance Criteria

1. WHEN Scb 엔티티가 조회되면, THE Loan_Application_Grade_API SHALL Scb의 s_grade 정수 값(1~10)을 SGrade 열거형에 매핑(1→S1, 2→S2, ..., 10→S10)하여 해당 label 문자열을 sGrade 필드(String 타입)에 반환한다
2. IF Scb의 s_grade 값이 1~10 범위 밖이거나 SGrade 열거형에 매핑할 수 없는 값이면, THEN THE Loan_Application_Grade_API SHALL HTTP 404 상태 코드와 함께 isSuccess=false, code="COMMON4004", message="요청한 리소스를 찾을 수 없습니다."를 포함하는 공통 에러 응답을 반환한다

### Requirement 4: SCB 점수 정보(scbInfo) 조회

**User Story:** 은행원으로서, SCB 점수 구성(SCB 점수, 최대 점수, 가산점)을 확인하여, 성장 S등급이 신용점수에 미친 영향을 파악하고 싶다.

#### Acceptance Criteria

1. WHEN Scb 엔티티가 조회되면, THE Loan_Application_Grade_API SHALL scbInfo 섹션에 score(SCB 점수, Scb의 scb_grade 값, 0 이상 1000 이하 정수), maxScore(SCB 최대 점수, 1000 고정값, 정수), bonusPoints(가산점, SScoringRule의 score_addition 값, 0 이상 정수) 필드를 포함한다
2. THE Loan_Application_Grade_API SHALL bonusPoints를 조회할 때 Scb의 s_grade 값을 SGrade label로 변환한 후, SScoringRule 엔티티에서 grade = SGrade label인 레코드의 score_addition 값을 사용한다
3. THE Loan_Application_Grade_API SHALL scbInfo의 score 값이 cbScore 섹션의 score 값과 bonusPoints 값의 합(cb_grade + score_addition)과 일치하도록 Scb 엔티티에 저장된 scb_grade 값을 그대로 반환한다
4. IF SScoringRule에서 해당 등급의 score_addition 값이 0이면, THEN THE Loan_Application_Grade_API SHALL bonusPoints 필드를 정수 0으로 반환한다

### Requirement 5: SHAP 분석 결과(shapResult) 조회

**User Story:** 은행원으로서, SHAP 기반 분석 결과를 확인하여, 성장 S등급 산출 근거를 상세히 파악하고 싶다.

#### Acceptance Criteria

1. WHEN LoanApplication이 조회되면, THE Loan_Application_Grade_API SHALL LoanApplication의 s_evaluation_id로 ShapExplanation 엔티티(evaluation_id = s_evaluation_id)를 조회하여 shapResult 섹션을 구성한다
2. THE Loan_Application_Grade_API SHALL shapResult에 grade(현재 S등급, ShapExplanation의 s_grade를 SGrade label 문자열로 변환), targetGrade(목표 S등급, ShapExplanation의 target_grade를 SGrade label 문자열로 변환) 필드를 포함한다
3. THE Loan_Application_Grade_API SHALL shapResult에 strengthKeywords(강점 키워드 목록, 문자열 배열)와 improvementKeywords(개선점 키워드 목록, 문자열 배열) 필드를 포함한다
4. THE Loan_Application_Grade_API SHALL shapResult에 strengthDetails와 improvementDetails 필드를 Map<String, Double> 형태로 포함하되, ShapExplanation의 strength_details 및 improvement_details(List<String>, 각 원소 형식: "피처명:SHAP점수")를 구분자 ":"로 분리하여 키는 피처명 문자열, 값은 SHAP 점수(Double)로 변환한다
5. THE Loan_Application_Grade_API SHALL shapResult에 advice(AI 생성 개선 조언 텍스트, 문자열) 필드를 포함한다
6. IF ShapExplanation의 strengthKeywords 또는 improvementKeywords가 빈 목록이면, THEN THE Loan_Application_Grade_API SHALL 해당 필드를 빈 배열([])로 반환한다
7. IF ShapExplanation의 strengthDetails 또는 improvementDetails가 빈 목록이면, THEN THE Loan_Application_Grade_API SHALL 해당 필드를 빈 객체({})로 반환한다
8. IF strength_details 또는 improvement_details의 원소가 "피처명:SHAP점수" 형식에 맞지 않아 파싱에 실패하면, THEN THE Loan_Application_Grade_API SHALL 해당 원소를 무시하고 파싱 가능한 원소만으로 Map을 구성한다

### Requirement 6: 존재하지 않는 신청 건 에러 처리

**User Story:** 은행원으로서, 존재하지 않는 applicationId로 조회 시 명확한 에러 메시지를 받아, 잘못된 접근임을 인지하고 싶다.

#### Acceptance Criteria

1. IF applicationId가 유효하지 않은 형식(숫자가 아닌 값 또는 Long 타입 범위(1 ~ 9,223,372,036,854,775,807)를 초과하는 값)이면, THEN THE Loan_Application_Grade_API SHALL HTTP 400 상태 코드와 함께 isSuccess=false, code="COMMON4000", message="잘못된 요청입니다."를 포함하는 공통 에러 응답을 반환한다
2. WHEN 존재하지 않는 applicationId로 GET /api/admin/loan-applications/{applicationId}/grade 요청이 수신되면, THE Loan_Application_Grade_API SHALL HTTP 404 상태 코드와 함께 isSuccess=false, code="COMMON4004", message="요청한 리소스를 찾을 수 없습니다."를 포함하는 공통 에러 응답을 반환한다
3. IF applicationId에 해당하는 LoanApplication은 존재하나 해당 application_id에 연관된 Scb 엔티티가 존재하지 않으면, THEN THE Loan_Application_Grade_API SHALL HTTP 404 상태 코드와 함께 isSuccess=false, code="COMMON4004", message="요청한 리소스를 찾을 수 없습니다."를 포함하는 공통 에러 응답을 반환한다
4. IF LoanApplication의 s_evaluation_id가 null이거나 해당 evaluation_id에 대응하는 ShapExplanation이 존재하지 않으면, THEN THE Loan_Application_Grade_API SHALL HTTP 404 상태 코드와 함께 isSuccess=false, code="COMMON4004", message="요청한 리소스를 찾을 수 없습니다."를 포함하는 공통 에러 응답을 반환한다
5. THE Loan_Application_Grade_API SHALL 에러 검증을 다음 순서로 수행한다: applicationId 형식 검증(400) → LoanApplication 존재 검증(404) → Scb 존재 검증(404) → ShapExplanation 존재 검증(404). 선행 검증 실패 시 후속 검증을 수행하지 않고 즉시 에러 응답을 반환한다

### Requirement 7: 인증 및 권한 검증

**User Story:** 은행원으로서, 인증된 관리자만 성장 S등급 정보를 조회할 수 있어, 고객의 신용 정보가 보호되길 원한다.

#### Acceptance Criteria

1. IF 세션 쿠키가 없거나 세션이 만료 또는 무효한 요청이 수신되면, THEN THE Loan_Application_Grade_API SHALL HTTP 401 상태 코드와 함께 isSuccess=false, code="COMMON4001", message="인증이 필요합니다."를 ApiResponse 공통 포맷으로 반환한다
2. IF 인증되었으나 BANK_ADMIN 권한이 없는 사용자가 요청하면, THEN THE Loan_Application_Grade_API SHALL HTTP 403 상태 코드와 함께 isSuccess=false, code="COMMON4003", message="권한이 없습니다."를 ApiResponse 공통 포맷으로 반환한다
3. THE Loan_Application_Grade_API SHALL 인증 검증을 권한 검증보다 먼저, 권한 검증을 비즈니스 로직 수행보다 먼저 완료하여, 인증 실패 시 HTTP 401을, 권한 실패 시 HTTP 403을 반환하고 대출 신청 데이터에 접근하지 않는다
4. IF 인증 또는 권한 검증이 실패하면, THEN THE Loan_Application_Grade_API SHALL 응답 본문의 result 필드를 null로 반환한다

### Requirement 8: SHAP Details 데이터 변환

**User Story:** 은행원으로서, SHAP 상세 데이터를 피처명-점수 쌍의 Map 형태로 받아, 각 피처의 기여도를 직관적으로 파악하고 싶다.

#### Acceptance Criteria

1. WHEN ShapExplanation의 strengthDetails가 조회되면, THE Loan_Application_Grade_API SHALL 저장된 List<String>의 각 요소를 콜론(:) 구분자로 분리하여 콜론 앞부분을 피처명(String) 키로, 콜론 뒷부분을 SHAP 점수(Double, 양수)로 파싱한 Map<String, Double> 형태로 변환하여 반환한다
2. WHEN ShapExplanation의 improvementDetails가 조회되면, THE Loan_Application_Grade_API SHALL 저장된 List<String>의 각 요소를 콜론(:) 구분자로 분리하여 콜론 앞부분을 피처명(String) 키로, 콜론 뒷부분을 SHAP 점수(Double, 음수)로 파싱한 Map<String, Double> 형태로 변환하여 반환한다
3. THE Loan_Application_Grade_API SHALL strengthDetails 및 improvementDetails의 모든 SHAP 점수 값을 반올림 없이 원본 데이터 그대로 반환한다
4. THE Loan_Application_Grade_API SHALL 변환된 Map의 피처 순서를 원본 List의 요소 순서와 동일하게 유지하여 반환한다(LinkedHashMap 사용)
5. IF strengthDetails 또는 improvementDetails의 List 요소가 콜론(:)을 포함하지 않거나 콜론 뒷부분이 숫자로 파싱 불가능한 경우, THEN THE Loan_Application_Grade_API SHALL 해당 요소를 건너뛰고 나머지 유효한 요소만으로 Map을 구성하여 반환한다
