# Requirements Document

## Introduction

은행원 대출 신청 상세보기의 심사 결과 탭 API를 개발합니다. 이 API는 대출 상품 정보, 신청 정보, 시스템 승인 정보(recommendation), 심사 이력(decisions)을 통합하여 은행원에게 제공합니다. `/api/admin/loan-applications/{applicationId}/review` 엔드포인트로 구현됩니다.

## Glossary

- **Review_Tab_API**: 은행원 대출 신청 상세보기에서 심사 결과 탭 데이터를 조회하는 GET API
- **Loan_Decision**: 심사 결과를 저장하는 엔티티 (loan_decision 테이블). created_by가 null이면 시스템 심사, null이 아니면 은행원 심사
- **Loan_Application**: 대출 신청 정보를 저장하는 엔티티 (loan_application 테이블)
- **Loan_Product**: 대출 상품 정보를 저장하는 엔티티 (loan_product 테이블)
- **Loan_Product_Option**: 대출 상품의 상환방식 및 대출목적 옵션을 저장하는 엔티티 (loan_product_options 테이블)
- **Product_Info**: 대출 상품의 기본 정보 (상품명, 금액 범위, 금리 범위, 기간 범위, 상환방식 목록, 대출목적 목록)
- **Application_Info**: 고객이 신청한 대출 조건 정보 (신청금액, 신청기간, 대출목적, 상환방식)
- **Recommendation**: 시스템 자동 승인 결과 정보 (승인금액, 승인금리, 승인기간, 상환방식)
- **Decision_History**: 시스템 심사와 은행원 심사를 통합한 심사 이력 목록
- **Converter**: Entity를 DTO로 변환하는 클래스

## Requirements

### Requirement 1: 심사 결과 탭 API 엔드포인트

**User Story:** 은행원으로서, 대출 신청 상세보기에서 심사 결과 탭을 조회하고 싶다. 그래서 상품 정보, 신청 정보, 시스템 승인 정보, 심사 이력을 한 번에 확인할 수 있다.

#### Acceptance Criteria

1. WHEN GET 요청이 `/api/admin/loan-applications/{applicationId}/review` 경로로 수신되면, THE Review_Tab_API SHALL Product_Info, Application_Info, Recommendation, Decision_History를 포함한 응답을 반환한다.
2. THE Review_Tab_API SHALL 공통 응답 포맷(ApiResponse)으로 응답을 래핑하여 반환한다.
3. WHEN 요청이 성공하면, THE Review_Tab_API SHALL HTTP 200 상태코드와 함께 `isSuccess: true`, `code: "COMMON2000"`, `message: "성공입니다."`를 반환한다.

### Requirement 2: 인증 및 권한 검증

**User Story:** 시스템 관리자로서, 심사 결과 탭 API에 인증된 관리자만 접근할 수 있도록 하고 싶다. 그래서 비인가 접근을 방지할 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 세션이 존재하지 않는 요청(세션 쿠키 누락 또는 Redis에 해당 세션이 존재하지 않는 경우)이 수신되면, THE Review_Tab_API SHALL HTTP 401 상태코드와 공통 응답 포맷(`isSuccess: false`, `code: "COMMON4001"`)을 반환한다.
2. IF 인증된 사용자가 ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER, ADMIN_DEV 권한 중 하나도 보유하지 않은 경우, THEN THE Review_Tab_API SHALL HTTP 403 상태코드와 공통 응답 포맷(`isSuccess: false`, `code: "COMMON4003"`)을 반환한다.
3. WHEN 요청 처리 중 세션이 만료된 경우(Redis에서 세션이 삭제된 경우), THE Review_Tab_API SHALL HTTP 401 상태코드와 공통 응답 포맷(`isSuccess: false`, `code: "COMMON4001"`)을 반환하고, 이후 요청을 처리하지 않는다.

### Requirement 3: 대출 신청 존재 검증

**User Story:** 은행원으로서, 존재하지 않는 대출 신청 ID로 조회 시 명확한 에러 메시지를 받고 싶다. 그래서 잘못된 요청을 인지할 수 있다.

#### Acceptance Criteria

1. WHEN 데이터베이스에 존재하지 않는 applicationId(Long 타입, 양의 정수)로 조회 요청이 수신되면, THE Review_Tab_API SHALL HTTP 404 상태코드와 응답 본문에 `isSuccess: false`, `code: "COMMON4004"`, `message: "요청한 리소스를 찾을 수 없습니다."`를 포함하는 ApiResponse를 반환한다.
2. IF applicationId 경로 변수가 Long 타입으로 변환할 수 없는 값(문자열, 소수점, 빈 값 등)으로 요청되면, THEN THE Review_Tab_API SHALL HTTP 400 상태코드와 `code: "COMMON4000"`을 반환한다.

### Requirement 4: 대출 상품 정보(Product_Info) 조회

**User Story:** 은행원으로서, 해당 대출 신청에 연결된 상품의 기본 정보를 확인하고 싶다. 그래서 심사 시 상품 조건을 참고할 수 있다.

#### Acceptance Criteria

1. WHEN 시스템에 존재하는 applicationId로 요청이 수신되면, THE Review_Tab_API SHALL Loan_Application의 product_id를 통해 Loan_Product에서 상품명(productName), 최소금액(min_limit), 최대금액(max_limit), 최소금리(min_rate), 최대금리(max_rate), 최소기간(min_term), 최대기간(max_term)을 조회하여 Product_Info에 포함한다.
2. WHEN 시스템에 존재하는 applicationId로 요청이 수신되면, THE Review_Tab_API SHALL Loan_Product_Option에서 해당 product_id의 모든 상환방식(repayment_method)을 중복 제거하여 availableRepaymentMethods 배열로 반환한다. 해당 product_id에 옵션이 존재하지 않으면 빈 배열을 반환한다.
3. WHEN 시스템에 존재하는 applicationId로 요청이 수신되면, THE Review_Tab_API SHALL Loan_Product_Option에서 해당 product_id의 모든 대출목적(purpose)을 중복 제거하여 availablePurposes 배열로 반환한다. 해당 product_id에 옵션이 존재하지 않으면 빈 배열을 반환한다.

### Requirement 5: 신청 정보(Application_Info) 조회

**User Story:** 은행원으로서, 고객이 신청한 대출 조건을 확인하고 싶다. 그래서 고객의 희망 조건을 파악할 수 있다.

#### Acceptance Criteria

1. WHEN loan_application 테이블에 존재하는 applicationId로 요청이 수신되면, THE Review_Tab_API SHALL Loan_Application에서 신청금액(requested_amount), 신청기간(requested_term), 대출목적(purpose), 상환방식(repayment_method)을 조회하여 Application_Info에 포함한다.
2. WHEN Loan_Application의 신청 조건 필드(requested_amount, requested_term, purpose, repayment_method) 중 값이 존재하지 않는 필드가 있으면, THE Review_Tab_API SHALL 해당 필드를 null로 포함하여 Application_Info를 반환한다.

### Requirement 6: 시스템 승인 정보(Recommendation) 조회

**User Story:** 은행원으로서, 시스템이 자동으로 산출한 승인 조건을 확인하고 싶다. 그래서 심사 판단의 참고 자료로 활용할 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 applicationId로 요청이 수신되면, THE Review_Tab_API SHALL Loan_Decision 중 created_by가 null인 레코드(시스템 심사)에서 승인금액(approved_amount), 승인금리(approved_rate), 승인기간(approved_term)을 조회하고, approved_term 값을 repaymentMethod 필드로 매핑하여 Recommendation에 포함한다.
2. IF 해당 applicationId에 대한 시스템 심사 Loan_Decision(created_by = null)이 존재하지 않으면, THEN THE Review_Tab_API SHALL Recommendation 필드를 null로 반환한다.
3. IF 해당 applicationId에 대한 시스템 심사 Loan_Decision의 decision이 REJECTED이면, THEN THE Review_Tab_API SHALL Recommendation 필드를 null로 반환한다.

### Requirement 7: 심사 이력(Decision_History) 조회

**User Story:** 은행원으로서, 시스템 심사와 은행원 심사의 전체 이력을 시간순으로 확인하고 싶다. 그래서 심사 진행 경과를 파악할 수 있다.

#### Acceptance Criteria

1. WHEN 유효한 applicationId로 요청이 수신되면, THE Review_Tab_API SHALL Loan_Decision의 모든 심사 이력을 조회하여 각 항목에 status, comment, reviewerName, reviewerRole, decidedAt을 포함한 decisions 배열로 반환한다.
2. THE Review_Tab_API SHALL decisions 배열을 심사 일시(decidedAt — Loan_Decision의 created_at을 매핑) 기준 오름차순(오래된 것 먼저)으로 정렬한다.
3. WHEN Loan_Decision의 created_by가 null인 경우(시스템 심사), THE Review_Tab_API SHALL reviewerName을 "시스템"으로, reviewerRole을 "SYSTEM"으로 고정 설정한다.
4. WHEN Loan_Decision의 created_by가 null이 아닌 경우(은행원 심사), THE Review_Tab_API SHALL created_by(user_id)로 users 테이블을 조회하여 reviewerName에 users.name을, reviewerRole에 users.role을 설정한다.
5. WHEN Loan_Decision의 created_by가 null이고 decision이 APPROVED이면, THE Review_Tab_API SHALL decisions의 status를 "SYSTEM_APPROVED"로 매핑한다.
6. WHEN Loan_Decision의 created_by가 null이 아닌 경우, THE Review_Tab_API SHALL Loan_Decision의 decision 값을 그대로 decisions의 status로 사용한다.
7. IF Loan_Decision의 created_by에 해당하는 사용자가 users 테이블에 존재하지 않으면, THEN THE Review_Tab_API SHALL reviewerName을 "알 수 없음"으로, reviewerRole을 "SYSTEM"으로 설정한다.
8. WHEN 해당 applicationId에 대한 심사 이력이 존재하지 않으면, THE Review_Tab_API SHALL decisions 필드를 빈 배열로 반환한다.

### Requirement 8: LoanDecision 엔티티 수정

**User Story:** 개발자로서, loan_decision 테이블의 created_at, created_by 컬럼을 엔티티에 매핑하고 싶다. 그래서 심사 이력 조회 시 심사자 정보와 심사 일시를 활용할 수 있다.

#### Acceptance Criteria

1. THE LoanDecision 엔티티 SHALL created_at(LocalDateTime, updatable=false) 필드를 추가하여 DB의 created_at 컬럼을 매핑한다.
2. THE LoanDecision 엔티티 SHALL created_by(Long, nullable, updatable=false) 필드를 추가하여 DB의 created_by 컬럼을 매핑한다. 시스템 심사의 경우 이 값은 null이다.

### Requirement 9: 응답 DTO 및 Converter 구현

**User Story:** 개발자로서, 심사 결과 탭 응답을 위한 DTO와 Converter를 구현하고 싶다. 그래서 프로젝트 컨벤션에 맞는 코드 구조를 유지할 수 있다.

#### Acceptance Criteria

1. THE Converter SHALL 각 중첩 DTO(ProductInfoResponse, ApplicationInfoResponse, RecommendationResponse, DecisionResponse)에 대해 Entity를 입력받아 해당 DTO를 반환하는 개별 정적 메서드와, 이들을 조합하여 최상위 LoanApplicationReviewResponse를 반환하는 정적 메서드를 제공한다.
2. THE Review_Tab_API SHALL 최상위 응답 DTO(LoanApplicationReviewResponse)와 모든 중첩 DTO(ProductInfoResponse, ApplicationInfoResponse, RecommendationResponse, DecisionResponse)를 Java record 타입으로 정의한다.
3. THE Review_Tab_API SHALL LoanApplicationReviewResponse를 최상위 record로 정의하고, productInfo(ProductInfoResponse), applicationInfo(ApplicationInfoResponse), recommendation(RecommendationResponse, nullable), decisions(List<DecisionResponse>) 필드로 응답 구조를 구성한다.
4. IF recommendation 데이터가 존재하지 않는 경우, THEN THE Converter SHALL recommendation 필드를 null로 설정하여 LoanApplicationReviewResponse를 생성한다.
5. THE Converter SHALL private 생성자를 선언하여 인스턴스화를 방지한다.
