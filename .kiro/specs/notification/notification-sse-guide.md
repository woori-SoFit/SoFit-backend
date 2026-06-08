# 알림 서비스 설계 및 SSE 기반 개발 가이드

## 1. 개요

사용자에게 대출 프로세스의 주요 단계를 실시간으로 알려주는 알림 서비스를 SSE(Server-Sent Events) 방식으로 구현한다.

### 알림 유형 (3가지)

| 유형 | 트리거 시점 | 제목 | 본문 |
|------|------------|------|------|
| `USER_LOAN_SUBMITTED` | 사용자가 대출 신청 완료 | 대출 신청 완료 | 대출 신청이 정상적으로 접수되었습니다 |
| `USER_LOAN_DECIDED` | 은행원이 심사 완료 | 대출 심사 완료 | 신청하신 대출 심사가 완료되었습니다 |
| `USER_LOAN_EXECUTED` | 사용자가 대출 실행 완료 | 대출 실행 완료 | 대출이 정상적으로 실행되었습니다 |

---

## 2. SSE란?

**Server-Sent Events** — 서버가 클라이언트에게 HTTP 연결을 유지하며 단방향으로 이벤트를 푸시하는 기술.

```
클라이언트                          서버
    │                                │
    │── GET /notifications/subscribe ▶│  연결 수립 후 유지
    │                                │
    │                         이벤트 발생
    │◀── Event Response ─────────────│
    │◀── Event Response ─────────────│
    │◀── Event Response ─────────────│
    │                                │
    │◀── Connection Closed ──────────│
```

### SSE vs WebSocket

| 항목 | SSE | WebSocket |
|------|-----|-----------|
| 방향 | 서버 → 클라이언트 단방향 | 양방향 |
| 프로토콜 | HTTP | WS |
| 구현 복잡도 | 낮음 | 높음 |
| 자동 재연결 | 브라우저 기본 지원 | 직접 구현 |
| 알림 서비스 적합성 | ✅ 적합 | 과함 |

> 알림은 서버 → 클라이언트 단방향이므로 SSE가 WebSocket보다 적합하다.

### 이벤트 감지 방식

SSE는 **전송 방식**이고, 이벤트를 감지하는 방법은 별개다.

현재 방식은 서비스 레이어에서 상태 업데이트 직후 `NotificationService.send()`를 **명시적으로 호출**하는 방식으로, DB가 자동으로 감지하는 게 아니다.

```java
@Transactional
public void submitApplication(...) {
    // 1. DB 업데이트
    application.updateStatus(ApplicationStatus.SUBMITTED);
    loanApplicationRepository.save(application);

    // 2. 개발자가 직접 호출 (자동 감지 X)
    notificationService.send(...);  // ← 여기서 SSE Event Response 발생
}
```

---

## 3. ERD

### notification 테이블

| 컬럼 | 한글명 | 타입 | NULL |
|------|--------|------|------|
| `notification_id` | 알림 ID (PK) | BIGINT | NOT NULL |
| `user_id` | 수신 사용자 ID | BIGINT | NOT NULL |
| `type` | 알림 유형 | ENUM | NOT NULL |
| `title` | 제목 | VARCHAR(100) | NOT NULL |
| `message` | 본문 | TEXT | NOT NULL |
| `application_id` | 연관 대출 신청 ID | BIGINT | NOT NULL |
| `is_read` | 읽음 여부 | BOOLEAN | NOT NULL |
| `created_at` | 생성 일시 | DATETIME | NOT NULL |
| `read_at` | 읽은 일시 | DATETIME | NULL |

### 설계 결정 사항

- `title`, `message`는 type과 1:1 고정 문구이나 **알림 이력 보존** 및 **문구 변경 시 프론트 배포 불필요**를 위해 DB에 저장
- `application_id`로 `loan_application.status` 조회 가능하므로 별도 `reference_type` 불필요
- 알림 클릭 시 `type` + `application_id` 조합으로 프론트가 페이지 라우팅 결정

```
USER_LOAN_SUBMITTED → 대출 진행 관리 페이지  (/loans/{application_id}/status)
USER_LOAN_DECIDED   → 심사 결과 페이지       (/loans/{application_id}/result)
USER_LOAN_EXECUTED  → 대출 실행 완료 페이지  (/loans/{application_id}/complete)
```

---

## 4. SseEmitterManager란?

Spring에서 SSE 연결을 표현하는 객체가 `SseEmitter`다. 클라이언트 한 명당 SseEmitter 하나가 생성되고, 이 객체를 통해 해당 클라이언트에게 이벤트를 푸시할 수 있다.

```
사용자A ──── SSE 연결 ────▶ SseEmitter (사용자A 전용)
사용자B ──── SSE 연결 ────▶ SseEmitter (사용자B 전용)
사용자C ──── SSE 연결 ────▶ SseEmitter (사용자C 전용)
```

`SseEmitterManager`는 여러 사용자의 SseEmitter를 **Map으로 관리**하는 컴포넌트다.

```java
// 내부 구조
Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
//  userId    해당 유저의 SSE 연결 객체
```

두 가지 역할만 한다.

```java
// 1. 구독 - 사용자가 앱 진입 시 SSE 연결 수립하고 Map에 저장
subscribe(userId) {
    SseEmitter emitter = new SseEmitter();
    emitters.put(userId, emitter);
    return emitter;
}

// 2. 전송 - 이벤트 발생 시 해당 userId의 emitter를 꺼내서 푸시
send(userId, payload) {
    SseEmitter emitter = emitters.get(userId);
    emitter.send(payload);
}
```

한마디로 **"어떤 유저에게 보낼지 찾아주는 우편함 관리자"** 역할이다.

---

## 5. 아키텍처

### 멀티모듈 구조

```
sofit-common   ← Notification 엔티티, NotificationType enum, NotificationPushRequest DTO
sofit-user     ← SSE 연결 관리 (SseEmitterManager), 알림 구독/조회 API, 대출신청·실행 완료 알림 발송
sofit-admin    ← 심사 완료 시 알림 DB INSERT 후 sofit-user로 HTTP 호출
```

### admin → user HTTP 호출이 필요한 이유

DB를 공유하더라도 **JVM은 분리**되어 있다. sofit-admin에서 status를 업데이트할 때 `SseEmitterManager.send()`를 직접 호출할 수 없는 이유가 바로 이것이다.

```
sofit-admin JVM                    sofit-user JVM
      │                                  │
status = APPROVED 업데이트               │
      │                                  │
send() 호출하고 싶은데...                 │
      │                                  │
SseEmitterManager 여기 없음 ❌      SseEmitterManager (Map) 여기 있음 ✅
```

따라서 sofit-admin은 DB 업데이트 후 sofit-user에게 HTTP로 위임해야 한다.

```
sofit-admin
    │
    ├── notification DB INSERT     (공유 DB라 직접 가능 ✅)
    └── SseEmitterManager.send()  (JVM 분리라 직접 불가 ❌)
              │
              ▼
         POST /internal/notifications/push  (HTTP 호출로 위임)
              │
              ▼
         sofit-user가 자신의 SseEmitterManager.send() 호출 ✅
```

### 전체 흐름

```
[사용자 앱 진입]
      │
      ▼
GET /api/notifications/subscribe  ← SSE 연결 수립
      │
      │  [대출 신청 완료 시 - sofit-user]
      │  loan_application.status = SUBMITTED
      │        │
      │        ▼
      │  NotificationService.send()
      │        │
      │        ├── notification DB INSERT
      │        └── SseEmitterManager.send()  ──────▶ SSE Event Response
      │
      │  [은행원이 심사 완료 시 - sofit-admin]
      │  loan_application.status = APPROVED / REJECTED
      │        │
      │        ├── notification DB INSERT
      │        └── POST /internal/notifications/push (HTTP 호출)
      │                    │
      │                    ▼
      │               sofit-user
      │                    └── SseEmitterManager.send()  ──▶ SSE Event Response
      │
      │  [대출 실행 완료 시 - sofit-user]
      │  loan_execution INSERT
      │        │
      │        ▼
      │  NotificationService.send()
      │        └── SseEmitterManager.send()  ──────▶ SSE Event Response
```

### 이벤트별 트리거 서버 정리

| 이벤트 | 트리거 서버 | SSE 발송 서버 | 방식 |
|--------|------------|--------------|------|
| 대출 신청 완료 | sofit-user | sofit-user | 직접 발송 |
| 대출 심사 완료 | sofit-admin | sofit-user | FeignClient 경유 |
| 대출 실행 완료 | sofit-user | sofit-user | 직접 발송 |

---

## 6. 구현 코드

### 6-1. sofit-common

**NotificationType.java**
```java
public enum NotificationType {
    USER_LOAN_SUBMITTED("대출 신청 완료", "대출 신청이 정상적으로 접수되었습니다"),
    USER_LOAN_DECIDED("대출 심사 완료", "신청하신 대출 심사가 완료되었습니다"),
    USER_LOAN_EXECUTED("대출 실행 완료", "대출이 정상적으로 실행되었습니다");

    private final String title;
    private final String message;

    NotificationType(String title, String message) {
        this.title = title;
        this.message = message;
    }
}
```

**Notification.java**
```java
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false)
    private Long applicationId;

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;

    @Builder
    public Notification(Long userId, NotificationType type, Long applicationId) {
        this.userId = userId;
        this.type = type;
        this.title = type.getTitle();
        this.message = type.getMessage();
        this.applicationId = applicationId;
        this.isRead = false;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsRead(LocalDateTime readAt) {
        this.isRead = true;
        this.readAt = readAt;
    }
}
```

**NotificationPushRequest.java**
```java
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPushRequest {
    private Long userId;
    private Long notificationId;
    private NotificationType type;
    private String title;
    private String message;
    private Long applicationId;
    private LocalDateTime createdAt;

    public static NotificationPushRequest from(Notification notification) {
        return NotificationPushRequest.builder()
            .userId(notification.getUserId())
            .notificationId(notification.getNotificationId())
            .type(notification.getType())
            .title(notification.getTitle())
            .message(notification.getMessage())
            .applicationId(notification.getApplicationId())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}
```

---

### 6-2. sofit-user

**SseEmitterManager.java**
```java
@Component
public class SseEmitterManager {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private static final long TIMEOUT = 30 * 60 * 1000L; // 30분

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        // 연결 직후 더미 이벤트 전송 (503 방지)
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected"));
        } catch (IOException e) {
            emitters.remove(userId);
        }

        return emitter;
    }

    public void send(Long userId, NotificationPushRequest payload) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return; // 오프라인 → DB에만 저장된 상태

        try {
            emitter.send(SseEmitter.event().name("notification").data(payload));
        } catch (IOException e) {
            emitters.remove(userId);
        }
    }
}
```

**NotificationController.java**
```java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseEmitterManager sseEmitterManager;
    private final NotificationService notificationService;

    // 앱 진입 시 SSE 구독
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal UserPrincipal user) {
        return sseEmitterManager.subscribe(user.getUserId());
    }

    // 미읽음 알림 조회 (앱 재진입 시 오프라인 동안 쌓인 알림 처리)
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnread(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(notificationService.getUnread(user.getUserId()));
    }

    // 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    // sofit-admin에서 호출하는 내부 API (외부 접근 차단 필요)
    @PostMapping("/internal/push")
    public ResponseEntity<Void> push(@RequestBody NotificationPushRequest request) {
        notificationService.push(request);
        return ResponseEntity.ok().build();
    }
}
```

**NotificationService.java**
```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseEmitterManager sseEmitterManager;

    // user 서버 자체 알림 발송 (대출 신청 완료, 대출 실행 완료)
    @Transactional
    public void send(Long userId, NotificationType type, Long applicationId) {
        Notification notification = Notification.builder()
            .userId(userId)
            .type(type)
            .applicationId(applicationId)
            .build();
        notificationRepository.save(notification);

        sseEmitterManager.send(userId, NotificationPushRequest.from(notification));
    }

    // sofit-admin으로부터 SSE 푸시 수신 (대출 심사 완료)
    public void push(NotificationPushRequest request) {
        sseEmitterManager.send(request.getUserId(), request);
    }

    // 미읽음 알림 조회
    public List<NotificationResponse> getUnread(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId)
            .stream().map(NotificationResponse::from).toList();
    }

    // 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow();
        notification.markAsRead(LocalDateTime.now());
    }
}
```

---

### 6-3. sofit-admin

**FeignClient**
```java
@FeignClient(name = "sofit-user", url = "${sofit.user.internal-url}")
public interface UserServerClient {

    @PostMapping("/api/notifications/internal/push")
    void pushNotification(@RequestBody NotificationPushRequest request);
}
```

**심사 완료 시 알림 발송**
```java
@Transactional
public void completeDecision(LoanDecision decision, LoanApplication application) {
    // 1. 심사 결과 저장
    loanDecisionRepository.save(decision);
    application.updateStatus(
        decision.getDecision() == Decision.APPROVED
            ? ApplicationStatus.APPROVED
            : ApplicationStatus.REJECTED
    );

    // 2. 알림 DB 저장
    Notification notification = Notification.builder()
        .userId(application.getUserId())
        .type(NotificationType.USER_LOAN_DECIDED)
        .applicationId(application.getApplicationId())
        .build();
    notificationRepository.save(notification);

    // 3. sofit-user로 HTTP 호출 → SseEmitterManager.send() 위임
    userServerClient.pushNotification(NotificationPushRequest.from(notification));
}
```

---

## 7. @TransactionalEventListener

### 왜 필요한가?

단순히 서비스 로직 안에서 `send()`를 직접 호출하면 트랜잭션 커밋 전에 알림이 나갈 수 있다.

```java
@Transactional
public void submitApplication(...) {
    application.updateStatus(SUBMITTED);
    loanApplicationRepository.save(application);
    notificationService.send(...);  // 커밋 전에 실행
    // 만약 이후 예외 발생 → DB 롤백됐는데 알림은 이미 나간 상태 ❌
}
```

`@TransactionalEventListener`를 사용하면 트랜잭션 **커밋이 확정된 후**에만 알림을 발송할 수 있다.

```java
// 1. 서비스에서 이벤트 발행
@Transactional
public void submitApplication(...) {
    application.updateStatus(SUBMITTED);
    loanApplicationRepository.save(application);
    eventPublisher.publishEvent(new LoanSubmittedEvent(application)); // 예약만 해둠
}

// 2. 커밋 확정 후 실행
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleLoanSubmitted(LoanSubmittedEvent event) {
    // DB 롤백될 일 없이 확정된 후 알림 발송 ✅
    notificationService.send(
        event.getUserId(),
        NotificationType.USER_LOAN_SUBMITTED,
        event.getApplicationId()
    );
}
```

비즈니스 로직과 알림 발송이 분리되고, 트랜잭션 롤백 시 알림이 나가는 문제를 방지할 수 있다.

---

## 8. 오프라인 사용자 처리

SSE 연결이 없는 오프라인 상태에서는 DB에만 저장하고, 앱 재진입 시 미읽음 알림을 조회한다.

```
앱 재진입
    │
    ▼
GET /api/notifications/unread  ← 오프라인 동안 쌓인 알림 조회
    │
    ▼
미읽음 알림 뱃지 표시
    │
    ▼
GET /api/notifications/subscribe  ← SSE 재연결
    │
    ▼
이후 알림은 실시간 수신
```

---

## 9. 보안 주의사항

`/api/notifications/internal/push`는 sofit-admin 서버만 호출 가능해야 한다. Security Config에서 내부 IP만 허용하거나 별도 포트로 분리할 것.

```java
.requestMatchers("/api/notifications/internal/**")
    .access(new WebExpressionAuthorizationManager(
        "hasIpAddress('${sofit.admin.internal-ip}')"
    ))
```
