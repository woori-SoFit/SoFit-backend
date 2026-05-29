# 고객 정보 목록 조회 API - tasks.md

## 브랜치/커밋 정보
- 브랜치: `feat/SOFIT-150-고객-정보-목록-조회-API`
- 커밋: `[SOFIT-150] Feat: 고객 정보 목록 조회 API 구현`

---

## Phase 1: DTO 및 SuccessCode 정의

### 작업 내용
1. `DevSuccessCode.java` - dev 도메인 SuccessCode enum 생성
2. `UserListResponse.java` - 목록 응답 DTO (record)
3. `UserItemResponse.java` - 개별 사용자 응답 DTO (record)

### 파일 목록
- `sofit-admin/src/main/java/com/sofit/admin/domain/dev/exception/DevSuccessCode.java`
- `sofit-admin/src/main/java/com/sofit/admin/domain/dev/dto/response/UserListResponse.java`
- `sofit-admin/src/main/java/com/sofit/admin/domain/dev/dto/response/UserItemResponse.java`

---

## Phase 2: Repository 확장 (Specification 기반 동적 쿼리)

### 작업 내용
1. `UserRepository`에 `JpaSpecificationExecutor` 추가
2. `UserSpecification.java` - 검색 조건 동적 쿼리 Specification 생성

### 파일 목록
- `sofit-common/src/main/java/com/sofit/common/repository/user/UserRepository.java` (수정)
- `sofit-admin/src/main/java/com/sofit/admin/domain/dev/repository/UserSpecification.java`

---

## Phase 3: Converter, Service, Controller 구현

### 작업 내용
1. `DevUserConverter.java` - Entity → DTO 변환
2. `DevUserService.java` - 인터페이스
3. `DevUserServiceImpl.java` - 구현체 (페이징 + 필터 조회)
4. `DevUserControllerDocs.java` - Swagger 문서 인터페이스
5. `DevUserController.java` - 컨트롤러

### 파일 목록
- `sofit-admin/src/main/java/com/sofit/admin/domain/dev/converter/DevUserConverter.java`
- `sofit-admin/src/main/java/com/sofit/admin/domain/dev/service/DevUserService.java`
- `sofit-admin/src/main/java/com/sofit/admin/domain/dev/service/DevUserServiceImpl.java`
- `sofit-admin/src/main/java/com/sofit/admin/domain/dev/controller/DevUserControllerDocs.java`
- `sofit-admin/src/main/java/com/sofit/admin/domain/dev/controller/DevUserController.java`

---

## 권한 체크
- `AdminRoleService.getCurrentUserRole()` 사용
- ADMIN_DEV, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER만 접근 가능
- 그 외 → `GeneralErrorCode.FORBIDDEN` 예외

## 조회 조건
- keyword: 이름(name) 또는 아이디(loginId) 부분 매칭 (LIKE)
- role: UserRole enum 필터
- status: UserStatus enum 필터
- page: 1부터 시작 (JPA는 0-based이므로 page-1 처리)
- size: null이면 기본값 8
