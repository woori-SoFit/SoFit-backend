# SoFit Backend

소상공인 대출 플랫폼 **SoFit**의 백엔드 모노레포입니다.<br>
**SoFit**은 기존 신용평가 체계에서 소외된 소상공인에게 ML 기반 성장 S등급(S1~S10)을 활용하여 대출 접근성을 높이는 플랫폼입니다.

<img width="960" height="540" alt="우리FISA 6기 - SOFIT 최종발표" src="https://github.com/user-attachments/assets/2b8c5088-a355-41d7-9fb1-536e53581b5b" />

<br>

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Build Tool | Gradle (멀티모듈) |
| ORM | Spring Data JPA |
| Database | MySQL |
| Session Store | Redis |
| Batch | Spring Batch |
| Auth | Spring Security (세션 기반) |
| API Docs | Swagger (springdoc-openapi) |
| Test | JUnit 5 + Mockito |
| CI/CD | Jenkins |

<br>

## 📁 모듈 구조

```text
SoFit-backend/
├── sofit-common/   # 공통 엔티티, 예외, 응답 포맷, 유틸
├── sofit-user/     # USER 대상 API 서버
├── sofit-admin/    # BANK_ADMIN, DEV_ADMIN 대상 API 서버
├── scripts/        # 배포 스크립트, SQL
├── docs/           # 작업 문서
└── k6/             # 부하 테스트
```

| 모듈 | 설명 | 포트 |
|------|------|------|
| `sofit-common` | 공통 Entity, Repository, DTO, 예외 처리, 유틸리티 | - |
| `sofit-user` | 고객용 API (회원가입, 대출 신청, My Biz Data, 알림) | 8080 |
| `sofit-admin` | 관리자용 API (대출 심사, 사용자 관리, 배치, API 로그) | 8081 |

<br>

## 🏗 주요 도메인

| 도메인 | 설명 |
|--------|------|
| `auth` | 로그인, 회원가입, PIN 인증 |
| `user` | 내 정보 조회, 회원 탈퇴 |
| `loan` | 대출 상품 조회, 신청, 심사, 약정, 실행 |
| `mybiz` | My Biz Data 수집 및 대시보드 |
| `report` | 성장 S등급 분석 리포트 |
| `notification` | 실시간 알림 (SSE + Redis Pub/Sub) |
| `product` | 대출 상품 관리 (Admin) |
| `dev` | 사용자 관리, API 로그 조회 (Admin) |

<br>

## 🔐 인증 및 권한

SoFit Backend는 **Spring Security 기반 세션 인증 방식**을 사용합니다.  
로그인 성공 시 세션이 생성되며, 세션 정보는 Redis에 저장하여 서버 간 인증 상태를 공유합니다.

### 권한 종류

| 권한 | 설명 |
|------|------|
| `USER` | 일반 고객 사용자 |
| `BANK_ADMIN` | 은행 관리자 |
| `DEV_ADMIN` | 개발 관리자 |

### API 경로 규칙

| 구분 | 경로 | 설명 |
|------|------|------|
| 고객용 API | `/api/**` | 일반 사용자 대상 API |
| 관리자용 API | `/api/admin/**` | 은행 관리자 및 개발 관리자 대상 API |
| 인증 API | `/api/auth/**` | 로그인, 회원가입, 세션 인증 관련 API |

<br>

## 📋 API 응답 포맷

모든 API는 공통 응답 포맷을 따릅니다.

### 성공 응답

```json
{
  "isSuccess": true,
  "code": "COMMON2000",
  "message": "성공입니다.",
  "result": {}
}
```

### 실패 응답

```json
{
  "isSuccess": false,
  "code": "COMMON4000",
  "message": "요청에 실패했습니다.",
  "result": null
}
```

<br>

## ✨ 주요 기능
<img width="960" height="540" alt="우리FISA 6기 - SOFIT 최종발표 (1)" src="https://github.com/user-attachments/assets/6e25cf39-ede9-4f25-999e-e0fb2ca0935a" />

| 기능 | 설명 |
|------|------|
| 회원 인증 | 회원가입, 로그인, 세션 인증, PIN 인증 |
| 대출 상품 조회 | 사용자에게 적합한 소상공인 대출 상품 조회 |
| 대출 신청 및 심사 | 대출 신청, 심사 요청, 승인/거절, 약정, 실행 프로세스 관리 |
| My Biz Data 연동 | 소상공인의 매출, 리뷰, 온라인 활동 데이터 수집 및 분석 |
| 성장 S등급 리포트 | ML 기반 성장 S등급 산출 결과와 맞춤형 분석 리포트 제공 |
| 실시간 알림 | SSE 기반 실시간 알림 및 Redis Pub/Sub 기반 이벤트 브로드캐스트 |
| 관리자 기능 | 대출 상품 관리, 사용자 관리, 대출 심사, API 로그 조회 |
| 배치 처리 | Spring Batch 기반 일일/월별 성장 S등급 산출 및 갱신 |

<br>

## 🧩 핵심 기술 구현

| 기술 | 해결한 문제 | 구현 내용 |
|------|------------|-----------|
| 세션 기반 인증 | 멀티 인스턴스 환경에서 사용자 인증 상태를 공유해야 하는 문제 | Spring Session과 Redis를 활용하여 세션을 외부 저장소에 저장하고, 여러 서버 인스턴스 간 동일한 인증 상태를 유지 |
| Redis Pub/Sub | SSE 연결 인스턴스와 알림 발생 인스턴스가 다를 경우 알림이 유실될 수 있는 문제 | 알림 이벤트를 Redis 채널에 Publish하고, 각 인스턴스가 Subscribe하여 SSE 연결을 보유한 인스턴스만 알림 전송 |
| Circuit Breaker | 외부 API 서버 장애가 전체 서비스 장애로 전파될 수 있는 문제 | 외부 API 호출 실패를 감지하고, 장애 발생 시 요청을 차단한 뒤 일정 시간 후 Half-Open 상태에서 복구 여부 확인 |
| SSE 실시간 알림 | 대출 신청, 심사, 실행 상태 변경을 사용자에게 실시간으로 전달해야 하는 문제 | Server-Sent Events를 활용하여 서버에서 클라이언트로 단방향 실시간 알림 전송 |
| Spring Batch | 성장 S등급 산출 및 갱신 작업을 정기적으로 실행해야 하는 문제 | 일일/월별 배치를 통해 사용자 성장 S등급 산출 및 리포트 갱신 자동화 |

<img width="320" alt="우리FISA 6기 - SOFIT 최종발표" src="https://github.com/user-attachments/assets/db77f018-6964-44f8-8e74-ef0415610566" />
<img width="320" alt="우리FISA 6기 - SOFIT 최종발표 (2)" src="https://github.com/user-attachments/assets/c2b64d17-6c54-49d7-b096-4e2db2f29061" />
<img width="320" alt="우리FISA 6기 - SOFIT 최종발표 (3)" src="https://github.com/user-attachments/assets/6a75746a-c7d3-4626-942a-16a5165f068e" />

<br><br>

## 🚀 로컬 실행 방법

### 사전 요구사항

- Java 17+
- Docker & Docker Compose (MySQL, Redis)
- Gradle 8.x

### 환경변수 설정

```bash
cp .env.example .env
# .env 파일에 DB 접속 정보, Redis 정보 등 입력
```

### 실행

```bash
# user-backend 실행
./gradlew :sofit-user:bootRun

# admin-backend 실행
./gradlew :sofit-admin:bootRun
```

### 빌드

```bash
./gradlew clean build
```

<br>

## 📄 관련 레포지토리

| 레포 | 설명 |
|------|------|
| SoFit-frontend | React 모노레포 (user, admin) |
| SoFit-AI | FastAPI + LightGBM 모델 |
| SoFit-DevOps | Docker, CI/CD 설정 |
| SoFit-external-mock | 외부 API Mock 서버 |
