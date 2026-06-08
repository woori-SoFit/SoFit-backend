# 고객 정보 통계 조회 API 구현

## 브랜치/커밋 정보
- 브랜치: `feat/SOFIT-XXX-user-statistics`
- 커밋: `[SOFIT-XXX] Feat: 고객 정보 통계 조회 API 구현`

---

## Phase 1: Response DTO 생성

| 파일 | 작업 |
|------|------|
| `sofit-admin/.../dev/dto/response/UserStatisticsResponse.java` | 통계 응답 DTO (record) |

---

## Phase 2: Repository 메서드 추가

| 파일 | 작업 |
|------|------|
| `sofit-common/.../repository/user/UserRepository.java` | 상태별/역할별 count 쿼리 메서드 추가 |

---

## Phase 3: Service 구현

| 파일 | 작업 |
|------|------|
| `sofit-admin/.../dev/service/DevUserService.java` | `findUserStatistics()` 메서드 선언 |
| `sofit-admin/.../dev/service/DevUserServiceImpl.java` | 통계 조회 로직 구현 |

---

## Phase 4: Controller 구현

| 파일 | 작업 |
|------|------|
| `sofit-admin/.../dev/controller/DevUserControllerDocs.java` | Swagger 문서 추가 |
| `sofit-admin/.../dev/controller/DevUserController.java` | GET /api/admin/users/statistics 엔드포인트 + 권한 체크 |

---

## 권한 체크
- `AdminRoleService.getCurrentUserRole()` 사용
- ADMIN_DEV, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER만 접근 가능
- 그 외 역할은 `GeneralErrorCode.FORBIDDEN` 예외 발생
