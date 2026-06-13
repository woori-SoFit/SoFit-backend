# 알림 SSE Redis Pub/Sub 고도화

## 개요
- 서버 이중화 대응을 위해 SSE 알림 시스템에 Redis Pub/Sub 도입
- 기존: 단일 인스턴스 ConcurrentHashMap → 이중화 시 알림 유실 가능
- 변경: Redis Pub/Sub 채널을 통해 모든 인스턴스로 알림 브로드캐스트 → 해당 emitter를 보유한 인스턴스가 실제 전송

## 브랜치 / 커밋
- 브랜치: `feat/SOFIT-XXX-notification-redis-pubsub`
- 커밋: `[SOFIT-XXX] Feat: 알림 SSE Redis Pub/Sub 도입 (이중화 대응)`

---

## Phase 1: Redis Pub/Sub 설정 (Config)

### 작업 내용
1. `RedisNotificationConfig.java` 생성 (sofit-user/global/config/)
   - `RedisMessageListenerContainer` Bean 등록
   - `notification:push` 채널 구독 설정
   - Jackson 기반 `MessageListener` 어댑터 등록

### 수정/생성 파일
- [신규] `sofit-user/src/main/java/com/sofit/user/global/config/RedisNotificationConfig.java`

---

## Phase 2: Redis Pub/Sub Publisher 구현

### 작업 내용
1. `RedisNotificationPublisher.java` 생성 (sofit-user/domain/notification/service/)
   - `RedisTemplate`을 이용하여 `notification:push` 채널에 `NotificationPushRequest` 발행
   - JSON 직렬화 처리

### 수정/생성 파일
- [신규] `sofit-user/src/main/java/com/sofit/user/domain/notification/service/RedisNotificationPublisher.java`

---

## Phase 3: Redis Pub/Sub Subscriber 구현

### 작업 내용
1. `RedisNotificationSubscriber.java` 생성 (sofit-user/domain/notification/service/)
   - `MessageListener` 구현
   - Redis 메시지 수신 → `NotificationPushRequest` 역직렬화
   - `SseEmitterManager.send()` 호출 (로컬 인스턴스의 emitter에만 전송)

### 수정/생성 파일
- [신규] `sofit-user/src/main/java/com/sofit/user/domain/notification/service/RedisNotificationSubscriber.java`

---

## Phase 4: 기존 코드 수정 — Pub/Sub 경유로 전환

### 작업 내용
1. `NotificationServiceImpl.send()` 수정
   - 기존: DB 저장 후 `sseEmitterManager.send()` 직접 호출
   - 변경: DB 저장 후 `redisNotificationPublisher.publish()` 호출
   - 이로써 모든 인스턴스에 메시지가 브로드캐스트됨

2. `NotificationServiceImpl.push()` 수정
   - 기존: `sseEmitterManager.send()` 직접 호출 (admin→user 내부 API)
   - 변경: `redisNotificationPublisher.publish()` 호출

3. `SseEmitterManager` 변경 없음 (로컬 emitter 관리 역할 유지)

### 수정 파일
- [수정] `sofit-user/src/main/java/com/sofit/user/domain/notification/service/NotificationServiceImpl.java`

---

## Phase 5: Heartbeat 스케줄러 추가 (보너스)

### 작업 내용
1. `SseEmitterManager`에 `@Scheduled` heartbeat 추가
   - 25초마다 comment("heartbeat") 전송 → Nginx/ALB idle timeout(60s) 방지
   - 전송 실패 시 해당 emitter 제거

2. `SofitUserApplication`에 `@EnableScheduling` 추가 (미등록 시)

### 수정 파일
- [수정] `sofit-user/src/main/java/com/sofit/user/domain/notification/service/SseEmitterManager.java`
- [수정] `sofit-user/src/main/java/com/sofit/user/SofitUserApplication.java` (필요시)

---

## 변경 영향도

| 구간 | 변경 사항 |
|------|----------|
| admin → user 내부 API (`/internal/push`) | 변경 없음 (API 인터페이스 유지) |
| 프론트 SSE 클라이언트 | 변경 없음 (동일 엔드포인트, 동일 이벤트 포맷) |
| Redis 인프라 | 기존 세션용 Redis 재활용 — 추가 인스턴스 불필요 |
| sofit-admin 코드 | 변경 없음 |

## 아키텍처 (변경 후)

```
[알림 발생]
  → NotificationServiceImpl.send()
  → DB 저장 (Notification)
  → RedisNotificationPublisher.publish("notification:push", payload)
  → Redis Pub/Sub 브로드캐스트

[모든 user-backend 인스턴스]
  ← RedisNotificationSubscriber.onMessage()
  → SseEmitterManager.send(userId, payload)
     → 해당 userId의 emitter가 로컬에 있으면 SSE 전송
     → 없으면 무시 (다른 인스턴스가 처리)
```
