# Requirements Document

## Introduction

은행원(BANK_ADMIN)이 대출 신청 건의 공통 정보를 상세 조회하는 API를 제공한다. 대출 대시보드 목록에서 특정 신청 건을 선택했을 때, 신청자명, 사업장명, 상품명, 심사 상태, 신청일, 담당 은행원명 등의 공통 정보를 단건으로 반환한다.

## Glossary

- **Loan_Application_Detail_API**: 대출 신청 상세보기(공통 정보) API. GET /api/admin/loan-applications/{applicationId} 엔드포인트로 제공되는 단건 조회 API
- **LoanApplication**: loan_application 테이블에 매핑되는 대출 신청 엔티티
- **User**: users 테이블에 매핑되는 사용자 엔티티. 신청자 및 은행원 정보를 포함
- **BusinessProfile**: business_profile 테이블에 매핑되는 사업자 프로필 엔티티
- **LoanProduct**: loan_product 테이블에 매핑되는 대출 상품 엔티티
- **ApplicationStatus**: 대출 신청의 심사 단계를 나타내는 열거형 (SYSTEM_APPROVED, SYSTEM_HOLD, MANAGER_REVIEW, APPROVED, REJECTED)
- **ApiResponse**: 공통 응답 포맷 래퍼 클래스. isSuccess, code, message, result 필드를 포함

## Requirements

### Requirement 1: 대출 신청 공통 정보 단건 조회

**User Story:** As a 은행원(BANK_ADMIN), I want 대출 신청 건의 공통 정보를 상세 조회하고 싶다, so that 심사 처리 전 신청자 및 신청 건의 기본 정보를 확인할 수 있다.

#### Acceptance Criteria

1. WHEN applicationId 경로 변수와 함께 GET /api/admin/loan-applications/{applicationId} 요청이 수신되면, THE Loan_Application_Detail_API SHALL LoanApplication 엔티티를 조회하여 공통 정보를 ApiResponse 형식(isSuccess: true, code, message, result)으로 반환한다
2. WHEN 대출 신청 공통 정보 조회에 성공하면, THE Loan_Application_Detail_API SHALL 응답 result에 applicationId(Long), applicantName(String), businessName(String), productName(String), status(ApplicationStatus enum 문자열), appliedAt(yyyy-MM-dd HH:mm:ss 형식 문자열), assignedBankerId(Long 또는 null), assigneeName(String 또는 null) 필드를 포함한다
3. WHEN LoanApplication 조회 시, THE Loan_Application_Detail_API SHALL loan_application의 user_id를 이용하여 User 엔티티에서 name을 조회하고 applicantName으로 반환한다
4. WHEN LoanApplication 조회 시, THE Loan_Application_Detail_API SHALL loan_application의 user_id를 이용하여 BusinessProfile 엔티티에서 business_name을 조회하고 businessName으로 반환한다
5. WHEN LoanApplication 조회 시, THE Loan_Application_Detail_API SHALL loan_application의 product_id를 이용하여 LoanProduct 엔티티에서 product_name을 조회하고 productName으로 반환한다
6. WHEN LoanApplication 조회 시 assigned_banker_id가 존재하면, THE Loan_Application_Detail_API SHALL 해당 ID로 User 엔티티에서 name을 조회하고 assigneeName으로 반환한다
7. WHEN LoanApplication 조회 시 assigned_banker_id가 null이면, THE Loan_Application_Detail_API SHALL assigneeName을 null로 반환한다

### Requirement 2: 존재하지 않는 대출 신청 건 처리

**User Story:** As a 은행원(BANK_ADMIN), I want 존재하지 않는 대출 신청 건 조회 시 명확한 에러 응답을 받고 싶다, so that 잘못된 요청에 대해 적절히 대응할 수 있다.

#### Acceptance Criteria

1. IF applicationId에 해당하는 LoanApplication이 존재하지 않으면, THEN THE Loan_Application_Detail_API SHALL HTTP 404 상태코드와 함께 isSuccess=false, code="COMMON4004", 그리고 리소스를 찾을 수 없음을 나타내는 message를 포함하는 에러 응답을 반환한다
2. IF applicationId가 양의 정수가 아닌 값(문자열, 음수, 0)으로 전달되면, THEN THE Loan_Application_Detail_API SHALL HTTP 400 상태코드와 함께 잘못된 요청임을 나타내는 에러 응답을 반환한다
3. IF applicationId에 해당하는 LoanApplication이 존재하지 않아 에러 응답을 반환하는 경우, THEN THE Loan_Application_Detail_API SHALL 응답 본문에 result 필드를 포함하지 않는다

### Requirement 3: 프로젝트 아키텍처 패턴 준수

**User Story:** As a 개발자, I want 대출 신청 상세 조회 API가 기존 프로젝트 아키텍처 패턴을 따르길 원한다, so that 코드 일관성과 유지보수성을 확보할 수 있다.

#### Acceptance Criteria

1. THE Loan_Application_Detail_API SHALL Controller(@RestController + @RequestMapping), Service 인터페이스, Service 구현체(@Service), Converter(static 메서드 + private 생성자), Response DTO(Java record 타입), ControllerDocs 인터페이스로 구성하며, Controller는 ControllerDocs 인터페이스를 implements 한다
2. THE Loan_Application_Detail_API SHALL Entity에서 Response DTO로의 변환을 Converter 클래스의 static 메서드에서 처리하며, Service 또는 Controller에서 직접 변환하지 않는다
3. THE Loan_Application_Detail_API SHALL Swagger 어노테이션(@Operation, @ApiResponse)을 ControllerDocs 인터페이스에 분리하여 정의하며, Controller 클래스에는 Swagger 어노테이션을 작성하지 않는다
4. WHEN 대출 신청 상세 조회 요청이 정상 처리되면, THE Loan_Application_Detail_API SHALL ApiResponse.onSuccess(SuccessCode, result) 형태로 공통 응답 포맷을 반환하며, SuccessCode는 도메인 exception 패키지 내 enum으로 정의한다
5. IF 조회 대상 대출 신청 건이 존재하지 않으면, THEN THE Loan_Application_Detail_API SHALL BaseException과 도메인 exception 패키지 내 ErrorCode를 사용하여 공통 에러 포맷으로 반환한다
