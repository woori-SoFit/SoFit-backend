# Requirements Document

## Introduction

은행원(BANK_ADMIN)이 대출 신청 상세보기 화면의 정보 탭에서 신청자 기본 정보, 사업자 정보, 대출 신청 정보, 고객 입력 정보, 약관 동의 이력을 한 번에 조회할 수 있는 API를 제공한다. 이 API는 `GET /api/admin/loan-applications/{applicationId}/info` 엔드포인트로 제공되며, 대출 심사 처리 시 은행원이 신청 건의 전체 맥락을 파악하는 데 사용된다.

## Glossary

- **Loan_Application_Info_API**: 대출 신청 상세보기 정보 탭 조회 API. applicationId를 Path Variable로 받아 해당 신청 건의 종합 정보를 반환하는 GET 엔드포인트
- **LoanApplication**: 대출 신청 엔티티. loan_application 테이블에 매핑되며 신청자(user_id), 상품, 신청 금액, 기간, 용도, 상환 방식, 고객 입력 정보 등을 포함
- **User**: 사용자 엔티티. users 테이블에 매핑되며 이름, 주민번호 앞 7자리, 전화번호, 로그인 ID, 가입일시 등을 포함
- **BusinessProfile**: 사업자 프로필 엔티티. business_profile 테이블에 매핑되며 상호명, 사업자등록번호, 업종, 업태, 주소, 개업일 등을 포함
- **ConsentHistory**: 약관 동의 이력 엔티티. consent_history 테이블에 매핑되며 사용자, 약관, 동의 여부, 동의 일시를 포함
- **Term**: 약관 엔티티. term 테이블에 매핑되며 약관 제목, 필수 여부, 약관 유형(TermType) 등을 포함
- **ApiResponse**: 공통 응답 포맷. isSuccess, code, message, result 필드로 구성

## Requirements

### Requirement 1: 대출 신청 정보 탭 조회

**User Story:** 은행원으로서, 대출 신청 건의 정보 탭을 조회하여, 신청자 기본 정보·사업자 정보·대출 신청 정보·고객 입력 정보·약관 동의 이력을 한 화면에서 확인하고 싶다.

#### Acceptance Criteria

1. WHEN applicationId를 Path Variable로 포함한 GET 요청이 수신되면, THE Loan_Application_Info_API SHALL LoanApplication, User, BusinessProfile, ConsentHistory, Term 엔티티를 조회하여 result 필드 내에 applicantInfo, businessInfo, applicationInfo, userInputInfo, consentHistories 5개 섹션을 포함하는 응답을 반환한다
2. THE Loan_Application_Info_API SHALL 응답을 ApiResponse 공통 포맷(isSuccess, code, message, result)으로 래핑하여 반환한다
3. WHEN 조회가 성공하면, THE Loan_Application_Info_API SHALL HTTP 200 상태 코드와 함께 isSuccess: true, code "COMMON2000", message "성공입니다."를 반환한다
4. WHEN 해당 LoanApplication에 연관된 ConsentHistory가 0건인 경우, THE Loan_Application_Info_API SHALL consentHistories 필드를 빈 배열([])로 반환한다

### Requirement 2: 신청자 기본 정보(applicantInfo) 조회

**User Story:** 은행원으로서, 대출 신청자의 기본 정보를 확인하여, 본인 확인 및 연락처를 파악하고 싶다.

#### Acceptance Criteria

1. WHEN LoanApplication이 조회되면, THE Loan_Application_Info_API SHALL LoanApplication의 user_id로 User 엔티티를 조회하여 applicantInfo 섹션을 구성한다
2. THE Loan_Application_Info_API SHALL applicantInfo에 name(이름, 문자열), residentNumber(주민번호 앞 7자리, 숫자 7자리 문자열), phoneNumber(전화번호, 하이픈 없는 11자리 숫자 문자열), joinedAt(가입일시), loginId(로그인 ID, 문자열) 필드를 포함한다
3. THE Loan_Application_Info_API SHALL joinedAt 필드를 ISO 8601 형식(yyyy-MM-ddTHH:mm:ss)으로 반환한다
4. IF LoanApplication의 user_id에 해당하는 User 엔티티가 존재하지 않으면, THEN THE Loan_Application_Info_API SHALL HTTP 404 상태 코드와 함께 GeneralErrorCode.NOT_FOUND(code: "COMMON4004", message: "요청한 리소스를 찾을 수 없습니다.")를 반환한다

### Requirement 3: 사업자 정보(businessInfo) 조회

**User Story:** 은행원으로서, 신청자의 사업자 정보를 확인하여, 사업 규모와 업종을 파악하고 싶다.

#### Acceptance Criteria

1. WHEN LoanApplication이 조회되면, THE Loan_Application_Info_API SHALL LoanApplication의 user_id로 BusinessProfile 엔티티를 조회하여 businessInfo 섹션을 구성한다
2. THE Loan_Application_Info_API SHALL businessInfo에 businessName(상호명), businessNumber(사업자등록번호, 하이픈 없는 10자리 숫자 문자열), businessCategory(업종), businessType(업태), businessAddress(사업장 주소), openDate(개업일) 필드를 포함한다
3. THE Loan_Application_Info_API SHALL openDate 필드를 ISO 8601 날짜 형식(yyyy-MM-dd)으로 반환한다
4. IF BusinessProfile의 선택적 필드(businessCategory, businessType, businessAddress, openDate)가 저장되지 않은 경우, THEN THE Loan_Application_Info_API SHALL 해당 필드를 null로 반환한다

### Requirement 4: 대출 신청 정보(applicationInfo) 조회

**User Story:** 은행원으로서, 고객이 신청한 대출 조건을 확인하여, 심사 기준 자료로 활용하고 싶다.

#### Acceptance Criteria

1. WHEN LoanApplication이 조회되면, THE Loan_Application_Info_API SHALL applicationInfo 섹션에 requestedAmount(희망 대출 금액, 원 단위 정수), requestedTerm(희망 대출 기간, 월 단위 정수), purpose(대출 용도), repaymentMethod(상환 방식) 필드를 포함한다
2. THE Loan_Application_Info_API SHALL purpose 필드를 LoanPurpose ENUM 값(WORKING_CAPITAL, FACILITY_CAPITAL) 그대로 반환한다
3. THE Loan_Application_Info_API SHALL repaymentMethod 필드를 RepaymentMethod ENUM 값(BULLET, EQUAL_PRINCIPAL, EQUAL_PAYMENT) 그대로 반환한다
4. THE Loan_Application_Info_API SHALL requestedAmount를 1 이상의 정수로 반환하고, requestedTerm을 1 이상의 정수(월 단위)로 반환한다

### Requirement 5: 고객 입력 정보(userInputInfo) 조회

**User Story:** 은행원으로서, 고객이 직접 입력한 소득·신용 정보를 확인하여, 심사 참고 자료로 활용하고 싶다.

#### Acceptance Criteria

1. WHEN LoanApplication이 조회되면, THE Loan_Application_Info_API SHALL userInputInfo 섹션에 annualIncome(연소득 구간), creditScore(신용점수 구간), incomeType(소득 유형), existingLoanAmount(기존 대출 금액 구간) 필드를 포함한다
2. THE Loan_Application_Info_API SHALL annualIncome 필드를 AnnualIncome ENUM의 name() 문자열 값(AMT_0_30M, AMT_30_50M, AMT_50_100M, AMT_100M_OVER 중 하나)으로 반환한다
3. THE Loan_Application_Info_API SHALL creditScore 필드를 CreditScoreRange ENUM의 name() 문자열 값(CS_0_850, CS_850_OVER, CS_UNKNOWN 중 하나)으로 반환한다
4. THE Loan_Application_Info_API SHALL incomeType 필드를 IncomeType ENUM의 code 값("01", "02", "03" 중 하나)으로 반환한다
5. THE Loan_Application_Info_API SHALL existingLoanAmount 필드를 ExistingLoanAmount ENUM의 name() 문자열 값(LOAN_100M_OVER, LOAN_0_100M, LOAN_NONE 중 하나)으로 반환한다
6. IF userInputInfo의 특정 필드(annualIncome, creditScore, incomeType, existingLoanAmount)가 LoanApplication 엔티티에서 null이면, THEN THE Loan_Application_Info_API SHALL 해당 필드를 null로 반환한다

### Requirement 6: 약관 동의 이력(consentHistories) 조회

**User Story:** 은행원으로서, 고객의 약관 동의 이력을 확인하여, 필수 약관 동의 여부를 검증하고 싶다.

#### Acceptance Criteria

1. WHEN LoanApplication이 조회되면, THE Loan_Application_Info_API SHALL 해당 LoanApplication의 user_id로 ConsentHistory를 조회하고, 각 ConsentHistory의 term_id로 Term 엔티티를 조회하여 consentHistories 배열을 구성한다
2. THE Loan_Application_Info_API SHALL consentHistories 배열의 각 항목에 title(약관 제목, Term에서 조회, 최대 100자), isRequired(필수 여부, Term에서 조회), isConsented(동의 여부, ConsentHistory에서 조회), consentedAt(동의 일시, ConsentHistory에서 조회) 필드를 포함한다
3. IF isConsented가 false인 경우, THEN THE Loan_Application_Info_API SHALL consentedAt 필드를 null로 반환한다
4. THE Loan_Application_Info_API SHALL consentedAt 필드를 ISO 8601 형식(yyyy-MM-ddTHH:mm:ss)으로 반환한다
5. IF 해당 LoanApplication의 user_id에 연결된 ConsentHistory가 존재하지 않으면, THEN THE Loan_Application_Info_API SHALL consentHistories 필드를 빈 배열([])로 반환한다
6. THE Loan_Application_Info_API SHALL consentHistories 배열을 ConsentHistory의 consent_id 오름차순으로 정렬하여 반환한다

### Requirement 7: 존재하지 않는 신청 건 에러 처리

**User Story:** 은행원으로서, 존재하지 않는 applicationId로 조회 시 명확한 에러 메시지를 받아, 잘못된 접근임을 인지하고 싶다.

#### Acceptance Criteria

1. WHEN 존재하지 않는 applicationId로 GET /api/admin/loan-applications/{applicationId}/info 요청이 수신되면, THE Loan_Application_Info_API SHALL HTTP 404 상태 코드와 함께 isSuccess=false, code="COMMON4004", message="요청한 리소스를 찾을 수 없습니다."를 포함하는 공통 에러 응답을 반환한다
2. IF applicationId가 유효하지 않은 형식(숫자가 아닌 값 또는 Long 타입 범위를 초과하는 값)이면, THEN THE Loan_Application_Info_API SHALL HTTP 400 상태 코드와 함께 isSuccess=false, code="COMMON4000", message="잘못된 요청입니다."를 포함하는 공통 에러 응답을 반환한다
3. IF 인증되지 않은 사용자가 요청하면, THEN THE Loan_Application_Info_API SHALL applicationId 유효성 검증 이전에 HTTP 401 상태 코드와 함께 isSuccess=false, code="COMMON4001", message="인증이 필요합니다."를 포함하는 공통 에러 응답을 반환한다

### Requirement 8: 인증 및 권한 검증

**User Story:** 은행원으로서, 인증된 관리자만 대출 신청 정보를 조회할 수 있어, 고객 정보가 보호되길 원한다.

#### Acceptance Criteria

1. IF 세션 쿠키가 없거나 세션이 만료 또는 무효한 요청이 수신되면, THEN THE Loan_Application_Info_API SHALL HTTP 401 상태 코드와 함께 GeneralErrorCode.UNAUTHORIZED(code: "COMMON4001", message: "인증이 필요합니다.")를 ApiResponse 공통 포맷으로 반환한다
2. IF 인증되었으나 BANK_ADMIN 권한이 없는 사용자가 요청하면, THEN THE Loan_Application_Info_API SHALL HTTP 403 상태 코드와 함께 GeneralErrorCode.FORBIDDEN(code: "COMMON4003", message: "권한이 없습니다.")를 ApiResponse 공통 포맷으로 반환한다
3. THE Loan_Application_Info_API SHALL 인증 및 권한 검증을 비즈니스 로직 수행 전에 완료하여, 인증/권한 실패 시 대출 신청 데이터에 접근하지 않는다
