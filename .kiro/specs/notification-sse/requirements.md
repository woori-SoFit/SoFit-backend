# Requirements Document

## Introduction

사용자에게 대출 프로세스의 주요 단계를 실시간으로 알려주는 알림 서비스를 SSE(Server-Sent Events) 방식으로 구현한다. 1차 구현 범위는 "대출 신청 완료(USER_LOAN_SUBMITTED)" 알림 유형만 포함하며, 이후 유형 추가가 용이한 구조로 설계한다.

## Glossary

- **Notification_Service**: 알림 생성, 저장, 조회, 읽음 처리를 담당하는 서비스 컴포넌트
- **SSE_Emitter_Manager**: ConcurrentHashMap으로 userId별 SseEmitter를 관리하며, SSE 구독 및 이벤트 전송을 담당하는 컴포넌트
- **Notification**: 알림 정보를 저장하는 엔티티 (notification_id, user_id, type, title, message, application_id, is_read, created_at, read_at)
- **NotificationType**: 알림 유형을 정의하는 enum (USER_LOAN_SUBMITTED, USER_LOAN_DECIDED, USER_LOAN_EXECUTED)
- **SseEmitter**: Spring에서 SSE 연결을 표현하는 객체로, 클라이언트 한 명당 하나가 생성되어 이벤트 푸시에 사용됨
- **NotificationPushRequest**: 알림 푸시 시 전달되는 DTO (userId, notificationId, type, title, message, applicationId, createdAt)
- **LoanSubmittedEvent**: 대출 신청 완료 시 발행되는 Spring 이벤트 객체

## Requirements

### Requirement 1: SSE 연결 구독

**User Story:** As a 사용자, I want to 앱 진입 시 SSE 연결을 수립하여, so that 서버로부터 실시간 알림을 수신할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 사용자가 SSE 구독 엔드포인트에 요청하면, THE SSE_Emitter_Manager SHALL 해당 userId에 대한 SseEmitter를 타임아웃 1,800,000ms(30분)로 생성하고 ConcurrentHashMap에 저장한 뒤 SseEmitter를 반환한다
2. WHEN SSE 연결이 수립되면, THE SSE_Emitter_Manager SHALL 연결 직후 name이 "connect"이고 data가 "connected"인 더미 이벤트를 전송한다
3. IF 더미 이벤트 전송 중 IOException이 발생하면, THEN THE SSE_Emitter_Manager SHALL 해당 userId의 SseEmitter를 ConcurrentHashMap에서 제거한다
4. WHEN 동일 userId로 SSE 구독 요청이 재수신되면, THE SSE_Emitter_Manager SHALL 기존 SseEmitter를 덮어쓰고 새로운 SseEmitter를 생성하여 저장한다
5. WHEN SseEmitter의 타임아웃(30분)이 발생하면, THE SSE_Emitter_Manager SHALL 해당 userId의 SseEmitter를 ConcurrentHashMap에서 제거한다
6. WHEN SseEmitter의 onCompletion 또는 onError 콜백이 호출되면, THE SSE_Emitter_Manager SHALL 해당 userId의 SseEmitter를 ConcurrentHashMap에서 제거한다
7. IF 인증되지 않은 사용자가 SSE 구독 엔드포인트에 요청하면, THEN THE System SHALL 해당 요청을 거부하고 인증이 필요함을 나타내는 에러 응답을 반환한다

### Requirement 2: 대출 신청 완료 알림 발송

**User Story:** As a 사용자, I want to 대출 신청 완료 시 실시간 알림을 받아, so that 신청이 정상 접수되었음을 즉시 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 대출 신청이 SUBMITTED 상태로 변경되는 트랜잭션이 커밋되면, THE Notification_Service SHALL userId, type(USER_LOAN_SUBMITTED), applicationId, title, message, isRead(false), createdAt을 포함하는 Notification을 생성하여 DB에 저장한다
2. WHEN Notification이 DB에 저장되면, THE SSE_Emitter_Manager SHALL 해당 userId의 SseEmitter를 통해 name이 "notification"이고 data가 NotificationPushRequest(userId, notificationId, type, title, message, applicationId, createdAt)인 SSE 이벤트를 전송한다
3. IF 해당 userId의 SseEmitter가 ConcurrentHashMap에 존재하지 않으면(오프라인), THEN THE Notification_Service SHALL DB 저장만 수행하고 SSE 전송을 건너뛴다
4. IF SSE 이벤트 전송 중 IOException이 발생하면, THEN THE SSE_Emitter_Manager SHALL 해당 userId의 SseEmitter를 ConcurrentHashMap에서 제거하고 예외를 전파하지 않는다
5. THE Notification_Service SHALL 트랜잭션 커밋이 확정된 후에만 알림 생성 및 발송을 수행하여, 트랜잭션 롤백 시 알림이 발송되지 않음을 보장한다

### Requirement 3: 미읽음 알림 조회

**User Story:** As a 사용자, I want to 앱 재진입 시 오프라인 동안 쌓인 미읽음 알림을 조회하여, so that 놓친 알림을 확인할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 사용자가 미읽음 알림 조회 엔드포인트에 요청하면, THE Notification_Service SHALL 해당 userId의 is_read가 false인 Notification 목록을 생성일시 내림차순으로 최대 100건까지 ApiResponse 형식으로 반환한다
2. THE Notification_Service SHALL 각 알림에 대해 notificationId, type, title, message, applicationId, createdAt 정보를 포함하여 반환한다
3. WHEN 인증된 사용자의 미읽음 알림이 0건일 때 조회 요청하면, THE Notification_Service SHALL 빈 목록을 ApiResponse 형식으로 반환한다
4. IF 인증되지 않은 사용자가 미읽음 알림 조회 엔드포인트에 요청하면, THEN THE Notification_Service SHALL 인증 실패를 나타내는 에러 응답을 반환한다

### Requirement 4: 알림 읽음 처리

**User Story:** As a 사용자, I want to 알림을 읽음 처리하여, so that 확인한 알림과 미확인 알림을 구분할 수 있다.

#### Acceptance Criteria

1. WHEN 인증된 사용자가 자신이 소유한 notificationId에 대해 읽음 처리를 요청하면, THE Notification_Service SHALL 해당 Notification의 is_read를 true로, read_at을 현재 시각으로 업데이트하고 ApiResponse 형식의 성공 응답을 반환한다
2. IF 존재하지 않는 notificationId로 읽음 처리를 요청하면, THEN THE Notification_Service SHALL 해당 리소스를 찾을 수 없음을 나타내는 에러 응답을 ApiResponse 형식으로 반환한다
3. IF 인증된 사용자가 자신이 소유하지 않은 다른 사용자의 notificationId로 읽음 처리를 요청하면, THEN THE Notification_Service SHALL 권한 없음을 나타내는 에러 응답을 ApiResponse 형식으로 반환하고 해당 Notification의 상태를 변경하지 않는다
4. IF 이미 is_read가 true인 notificationId로 읽음 처리를 요청하면, THEN THE Notification_Service SHALL read_at을 갱신하지 않고 ApiResponse 형식의 성공 응답을 반환한다

### Requirement 5: 내부 알림 푸시 API

**User Story:** As a sofit-admin 서버, I want to sofit-user 서버에 알림 푸시를 요청하여, so that 심사 완료 시 사용자에게 실시간 알림을 전달할 수 있다.

#### Acceptance Criteria

1. WHEN sofit-admin이 POST /api/notifications/internal/push 엔드포인트에 유효한 NotificationPushRequest(userId, notificationId, type, title, message, applicationId, createdAt)를 전송하면, THE SSE_Emitter_Manager SHALL 해당 userId의 SseEmitter를 통해 "notification" 이름의 SSE 이벤트를 전송한다
2. IF 해당 userId의 SSE 연결이 존재하지 않으면(오프라인 상태), THEN THE SSE_Emitter_Manager SHALL 이벤트 전송을 생략하고 정상 응답(200 OK)을 반환한다
3. THE Notification_Service SHALL /api/notifications/internal/push 엔드포인트를 세션 인증 없이 접근 가능하도록 SecurityConfig에서 permitAll로 설정하되, 운영 환경에서는 sofit-admin 서버 IP만 접근 가능하도록 IP 기반 접근 제한을 적용한다
4. IF NotificationPushRequest의 userId가 null이거나 필수 필드(notificationId, type, applicationId)가 누락된 경우, THEN THE Notification_Service SHALL 400 상태 코드와 함께 유효성 검증 실패를 나타내는 에러 응답을 반환한다

### Requirement 6: Notification 엔티티 및 테이블

**User Story:** As a 개발자, I want to 알림 데이터를 영구 저장하여, so that 알림 이력을 관리하고 오프라인 사용자에게도 알림을 제공할 수 있다.

#### Acceptance Criteria

1. THE Notification 엔티티 SHALL notification_id(PK, BIGINT, AUTO_INCREMENT), user_id(BIGINT, NOT NULL), type(VARCHAR(30), NOT NULL, EnumType.STRING으로 저장), title(VARCHAR(100), NOT NULL), message(TEXT, NOT NULL), application_id(BIGINT, NOT NULL), is_read(BOOLEAN, NOT NULL, DEFAULT FALSE), created_at(DATETIME, NOT NULL), read_at(DATETIME, NULL) 컬럼을 포함한다
2. THE Notification 엔티티 SHALL user_id와 is_read 컬럼에 대한 복합 인덱스(idx_notification_user_read)를 가진다
3. THE NotificationType enum SHALL USER_LOAN_SUBMITTED, USER_LOAN_DECIDED, USER_LOAN_EXECUTED 3개의 값을 정의하며, 각 값은 title(String)과 message(String) 필드를 포함한다
4. THE DDL 마이그레이션 파일 SHALL sofit-common/src/main/resources/db/migration/ 경로에 notification 테이블 생성 SQL을 포함하며, notification_id에 AUTO_INCREMENT PK, user_id와 is_read에 복합 인덱스를 정의한다

### Requirement 7: 알림 유형 확장 구조

**User Story:** As a 개발자, I want to 알림 유형을 쉽게 추가할 수 있는 구조를 갖추어, so that 향후 USER_LOAN_DECIDED, USER_LOAN_EXECUTED 유형을 최소 변경으로 추가할 수 있다.

#### Acceptance Criteria

1. THE NotificationType SHALL enum으로 정의되며, 각 유형은 비어 있지 않은 title(최대 100자)과 message 문구를 생성자 파라미터로 포함한다
2. WHEN 새로운 알림 유형을 추가할 때, THE NotificationType SHALL enum 값 추가만으로 NotificationService, SseEmitterManager, Controller 등 기존 코드 수정 없이 새 유형의 알림 발송이 가능한 구조를 제공한다
3. WHEN NotificationService.send()가 NotificationType을 파라미터로 받을 때, THE NotificationService SHALL type.getTitle()과 type.getMessage()를 사용하여 Notification 엔티티를 생성하므로, 새 유형 추가 시 수정이 필요한 파일은 NotificationType enum 1개로 제한된다
