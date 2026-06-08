# Implementation Plan: 알림 서비스 SSE (Notification SSE)

## Overview

사용자에게 대출 프로세스의 주요 단계를 실시간으로 알려주는 알림 서비스를 SSE(Server-Sent Events) 방식으로 구현한다. sofit-common에 Notification 엔티티/Repository/DTO를, sofit-user에 SSE 구독·알림 발송·조회·읽음 처리 로직을, sofit-admin에 RestTemplate 기반 내부 푸시 클라이언트를 구현한다. @TransactionalEventListener(AFTER_COMMIT)로 트랜잭션 커밋 후에만 알림을 발송하여 유령 알림을 방지한다.

## Tasks

- [ ] 1. sofit-common 엔티티 및 인프라 구성
  - [ ] 1.1 NotificationType enum 생성
    - `sofit-common/.../entity/notification/enums/NotificationType.java` 생성
    - USER_LOAN_SUBMITTED, USER_LOAN_DECIDED, USER_LOAN_EXECUTED 3개 값 정의
    - 각 값에 title(String), message(String) 필드 포함
    - _Requirements: 6.3, 7.1, 7.2_

  - [ ] 1.2 Notification 엔티티 생성
    - `sofit-common/.../entity/notification/Notification.java` 생성
    - notification_id(PK, AUTO_INCREMENT), user_id, type(EnumType.STRING), title, message, application_id, is_read(default false), created_at, read_at 컬럼 정의
    - @Index(idx_notification_user_read: user_id, is_read) 복합 인덱스 설정
    - Builder 패턴으로 생성 시 type.getTitle(), type.getMessage()로 title/message 자동 매핑
    - markAsRead(LocalDateTime) 메서드 구현
    - _Requirements: 6.1, 6.2, 7.3_

  - [ ] 1.3 NotificationRepository 생성
    - `sofit-common/.../repository/NotificationRepository.java` 생성
    - findTop100ByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId) 메서드 정의
    - _Requirements: 3.1, 3.2_

  - [ ] 1.4 NotificationPushRequest DTO 생성
    - `sofit-common/.../dto/notification/NotificationPushRequest.java` 생성
    - userId, notificationId, type, title, message, applicationId, createdAt 필드
    - @NotNull 검증 어노테이션 적용 (userId, notificationId, type, applicationId)
    - static from(Notification) 팩토리 메서드 구현
    - _Requirements: 5.1, 5.4_

  - [ ] 1.5 DDL 마이그레이션 파일 생성
    - `sofit-common/src/main/resources/db/migration/create_notification.sql` 생성
    - notification 테이블 CREATE + idx_notification_user_read 인덱스 정의
    - _Requirements: 6.4_

- [ ] 2. Checkpoint - 엔티티 및 공통 모듈 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 3. sofit-user 알림 서비스 핵심 로직 구현
  - [ ] 3.1 NotificationErrorCode / NotificationSuccessCode 생성
    - `sofit-user/.../domain/notification/exception/NotificationErrorCode.java` 생성
    - NOTIFICATION_NOT_FOUND(404, "NOTI4004"), NOTIFICATION_FORBIDDEN(403, "NOTI4003")
    - `sofit-user/.../domain/notification/exception/NotificationSuccessCode.java` 생성
    - UNREAD_LIST_OK("NOTI2000"), MARK_READ_OK("NOTI2001"), PUSH_OK("NOTI2002")
    - _Requirements: 4.2, 4.3_

  - [ ] 3.2 SseEmitterManager 구현
    - `sofit-user/.../domain/notification/service/SseEmitterManager.java` 생성
    - ConcurrentHashMap<Long, SseEmitter>으로 userId별 emitter 관리
    - subscribe(Long userId): SseEmitter 생성(타임아웃 30분), Map 저장, onCompletion/onTimeout/onError 콜백에서 제거, 더미 이벤트("connect", "connected") 전송
    - send(Long userId, NotificationPushRequest payload): emitter 존재 시 "notification" 이름으로 전송, IOException 시 제거 후 예외 전파 안 함
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 2.2, 2.4_

  - [ ] 3.3 NotificationConverter 생성
    - `sofit-user/.../domain/notification/converter/NotificationConverter.java` 생성
    - static toResponse(Notification) → NotificationResponse 변환
    - _Requirements: 3.2_

  - [ ] 3.4 NotificationResponse / NotificationListResponse DTO 생성
    - `sofit-user/.../domain/notification/dto/response/NotificationResponse.java` (record)
    - notificationId, type, title, message, applicationId, createdAt 필드
    - `sofit-user/.../domain/notification/dto/response/NotificationListResponse.java` (record)
    - List<NotificationResponse> notifications 필드
    - _Requirements: 3.1, 3.2_

  - [ ] 3.5 NotificationService 인터페이스 생성
    - `sofit-user/.../domain/notification/service/NotificationService.java` 생성
    - send(Long userId, NotificationType type, Long applicationId)
    - push(NotificationPushRequest request)
    - getUnread(Long userId) → NotificationListResponse
    - markAsRead(Long userId, Long notificationId)
    - _Requirements: 2.1, 3.1, 4.1, 5.1_

  - [ ] 3.6 NotificationServiceImpl 구현
    - `sofit-user/.../domain/notification/service/NotificationServiceImpl.java` 생성
    - send(): Notification 생성 → DB 저장 → SseEmitterManager.send() 호출
    - push(): SseEmitterManager.send() 호출 (DB 저장 없이 전송만)
    - getUnread(): Repository 조회 → NotificationConverter로 변환 → NotificationListResponse 반환
    - markAsRead(): 존재 확인 → 소유권 검증 → 이미 읽음이면 readAt 갱신 안 함 → 미읽음이면 markAsRead 호출
    - _Requirements: 2.1, 2.3, 3.1, 3.3, 4.1, 4.2, 4.3, 4.4, 5.1, 5.2_

- [ ] 4. sofit-user 이벤트 리스너 및 컨트롤러 구현
  - [ ] 4.1 LoanSubmittedEvent 생성
    - `sofit-user/.../domain/notification/event/LoanSubmittedEvent.java` 생성
    - userId, applicationId 필드 (불변 객체)
    - _Requirements: 2.1, 2.5_

  - [ ] 4.2 NotificationEventListener 구현
    - `sofit-user/.../domain/notification/service/NotificationEventListener.java` 생성
    - @TransactionalEventListener(phase = AFTER_COMMIT)으로 LoanSubmittedEvent 처리
    - notificationService.send(userId, USER_LOAN_SUBMITTED, applicationId) 호출
    - _Requirements: 2.1, 2.5_

  - [ ] 4.3 NotificationControllerDocs 인터페이스 생성
    - `sofit-user/.../domain/notification/controller/NotificationControllerDocs.java` 생성
    - @Tag(name = "Notification", description = "알림 API")
    - subscribe, getUnread, markAsRead, push 메서드에 @Operation 정의
    - _Requirements: 1.1, 3.1, 4.1, 5.1_

  - [ ] 4.4 NotificationController 구현
    - `sofit-user/.../domain/notification/controller/NotificationController.java` 생성
    - GET /api/notifications/subscribe (produces = TEXT_EVENT_STREAM_VALUE) → SseEmitter 반환
    - GET /api/notifications/unread → ApiResponse<NotificationListResponse>
    - PATCH /api/notifications/{notificationId}/read → ApiResponse<Void>
    - POST /api/notifications/internal/push → ApiResponse<Void>
    - @AuthenticationPrincipal CustomUserDetails로 인증 사용자 식별
    - _Requirements: 1.1, 1.7, 3.1, 3.4, 4.1, 5.1, 5.4_

  - [ ] 4.5 SecurityConfig에 내부 푸시 API permitAll 추가
    - `/api/notifications/internal/**` 경로를 permitAll로 설정
    - _Requirements: 5.3_

  - [ ] 4.6 LoanApplicationServiceImpl에 이벤트 발행 추가
    - submitApplication() 메서드에서 ApplicationEventPublisher.publishEvent(new LoanSubmittedEvent(userId, applicationId)) 호출
    - _Requirements: 2.1, 2.5_

- [ ] 5. Checkpoint - sofit-user 알림 서비스 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. sofit-admin 내부 푸시 클라이언트 구현
  - [ ] 6.1 RestTemplateConfig 생성
    - `sofit-admin/.../global/config/RestTemplateConfig.java` 생성
    - RestTemplate Bean 등록 (connectTimeout 5초, readTimeout 10초)
    - _Requirements: 5.1_

  - [ ] 6.2 NotificationPushClient 구현
    - `sofit-admin/.../domain/loan/client/NotificationPushClient.java` 생성
    - @Value("${sofit.user.internal-url}")로 sofit-user 서버 URL 주입
    - pushNotification(NotificationPushRequest): POST /api/notifications/internal/push 호출
    - 실패 시 로그만 남기고 예외 전파하지 않음 (알림 실패가 심사 처리를 막으면 안 됨)
    - _Requirements: 5.1, 5.2_

  - [ ] 6.3 sofit-admin application.yml에 sofit-user 내부 URL 설정 추가
    - `sofit.user.internal-url` 프로퍼티 추가 (local: http://localhost:8080, dev: 환경별 설정)
    - _Requirements: 5.1_

- [ ] 7. Checkpoint - 전체 모듈 통합 확인
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 8. Property-Based 테스트 작성
  - [ ]* 8.1 SseEmitterManager subscribe 덮어쓰기 Property 테스트
    - **Property 1: Subscribe 덮어쓰기 — Map에는 항상 최신 emitter만 존재**
    - **Validates: Requirements 1.1, 1.4**
    - jqwik으로 임의의 Long userId + 임의의 호출 횟수(1~10) 생성하여 마지막 반환 emitter와 Map 내 emitter 동일 객체 검증

  - [ ]* 8.2 SseEmitterManager 오프라인 전송 안전 건너뜀 Property 테스트
    - **Property 2: 오프라인 시 SSE 전송 안전 건너뜀**
    - **Validates: Requirements 2.3, 5.2**
    - jqwik으로 Map에 없는 임의의 Long userId + 임의의 NotificationPushRequest 생성하여 예외 미발생 검증

  - [ ]* 8.3 Notification 생성 시 type 기반 필드 매핑 Property 테스트
    - **Property 3: Notification 생성 시 type 기반 필드 매핑**
    - **Validates: Requirements 2.1, 7.3**
    - jqwik으로 임의의 NotificationType + userId + applicationId 생성하여 title == type.getTitle(), message == type.getMessage(), isRead == false, createdAt != null 검증

  - [ ]* 8.4 미읽음 알림 조회 조건 Property 테스트
    - **Property 4: 미읽음 알림 조회 조건 — 미읽음만, 내림차순, 최대 100건**
    - **Validates: Requirements 3.1, 3.2**
    - jqwik으로 임의의 Notification 리스트(0~200건, 읽음/미읽음 혼합) 생성하여 결과 검증

  - [ ]* 8.5 읽음 처리 상태 전환 Property 테스트
    - **Property 5: 읽음 처리 상태 전환**
    - **Validates: Requirements 4.1**
    - jqwik으로 isRead=false인 Notification 생성하여 markAsRead 후 isRead=true, readAt!=null 검증

  - [ ]* 8.6 읽음 처리 멱등성 Property 테스트
    - **Property 6: 읽음 처리 멱등성**
    - **Validates: Requirements 4.4**
    - jqwik으로 이미 isRead=true인 Notification 생성하여 markAsRead 재호출 시 readAt 미변경 검증

  - [ ]* 8.7 소유권 검증 Property 테스트
    - **Property 7: 소유권 검증 — 타인의 알림 읽음 처리 거부**
    - **Validates: Requirements 4.3**
    - jqwik으로 서로 다른 두 userId 생성하여 타인 알림 markAsRead 시 FORBIDDEN 예외 + 상태 미변경 검증

  - [ ]* 8.8 NotificationType enum 불변 조건 Property 테스트
    - **Property 8: NotificationType enum 불변 조건**
    - **Validates: Requirements 6.3, 7.1**
    - 모든 enum 값에 대해 getTitle() != null && !isEmpty() && length <= 100, getMessage() != null && !isEmpty() 검증

- [ ] 9. 단위 테스트 작성
  - [ ]* 9.1 SseEmitterManager 단위 테스트
    - SSE 구독 성공, 더미 이벤트 전송, 타임아웃 시 제거, 재구독 시 덮어쓰기 시나리오
    - _Requirements: 1.1, 1.2, 1.4, 1.5, 1.6_

  - [ ]* 9.2 NotificationServiceImpl 단위 테스트
    - send 정상 흐름, 오프라인 시 DB만 저장, getUnread 빈 목록, markAsRead 정상/미존재/권한없음/멱등성 시나리오
    - _Requirements: 2.1, 2.3, 3.1, 3.3, 4.1, 4.2, 4.3, 4.4_

  - [ ]* 9.3 NotificationController 단위 테스트
    - MockMvc로 각 엔드포인트 호출 테스트, 인증 없이 접근 시 401, 내부 푸시 필수 필드 누락 시 400
    - _Requirements: 1.7, 3.4, 5.4_

- [ ] 10. Final Checkpoint - 전체 테스트 통과 확인
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- 각 태스크는 특정 요구사항을 참조하여 추적 가능
- Checkpoint에서 점진적 검증 수행
- Property 테스트는 jqwik 라이브러리 사용, `@Tag("Feature: notification-sse, Property N: ...")` 태그 부착
- sofit-common에 엔티티/DTO/Repository를 배치하여 admin과 user 모듈 모두 접근 가능
- SSE 전송 실패는 예외를 전파하지 않음 (알림 실패가 비즈니스 로직을 중단시키면 안 됨)
- sofit-admin → sofit-user HTTP 호출 실패도 예외를 전파하지 않음 (심사 처리 보호)
- 내부 푸시 API는 SecurityConfig에서 permitAll 설정, 운영 환경에서는 IP 기반 접근 제한 추가 적용

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.5"] },
    { "id": 1, "tasks": ["1.2", "1.4"] },
    { "id": 2, "tasks": ["1.3", "3.1", "3.4"] },
    { "id": 3, "tasks": ["3.2", "3.3", "3.5"] },
    { "id": 4, "tasks": ["3.6", "4.1"] },
    { "id": 5, "tasks": ["4.2", "4.3"] },
    { "id": 6, "tasks": ["4.4", "4.5", "4.6"] },
    { "id": 7, "tasks": ["6.1", "6.3"] },
    { "id": 8, "tasks": ["6.2"] },
    { "id": 9, "tasks": ["8.1", "8.2", "8.3", "8.5", "8.6", "8.7", "8.8"] },
    { "id": 10, "tasks": ["8.4", "9.1", "9.2", "9.3"] }
  ]
}
```
