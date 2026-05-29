# 고객 정보 목록 조회 API - tasks.md

## 브랜치/커밋 정보
- 브랜치: `feat/SOFIT-150-고객-정보-목록-조회-API`
- 커밋: `[SOFIT-150] Feat: 고객 정보 목록 조회 API 구현`

---

## API 정보

- HTTP 메서드: GET
- Path: `/api/admin/users`
- Query Parameters: `page`, `size`, `keyword`, `role`, `status` (모두 optional)

## 파라미터 설명

| 파라미터 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| page | number |  | 페이지 번호 (0부터 시작, 기본값 0) |
| size | number |  | 페이지당 건수 (기본값 8) |
| keyword | string |  | 검색어 (이름, 아이디 부분 매칭) |
| role | string |  | 역할 필터 (ADMIN_DEV, ADMIN_BANK_MANAGER, ADMIN_BANK_TELLER, USER) |
| status | string |  | 상태 필터 (ACTIVE, INACTIVE) |

## 권한 체크
- Spring Security URL 기반 접근 제어 (`SecurityConfig`)
- `/api/admin/users/**` → ADMIN_DEV, ADMIN_BANK_TELLER, ADMIN_BANK_MANAGER 허용
- 컨트롤러에서 별도 권한 체크 없음

## 유효성 검증
- role, status에 유효하지 않은 값 전달 시 → `GeneralErrorCode.BAD_REQUEST` (400)

## 구현 파일 목록

### 생성
- `sofit-admin/.../dev/dto/response/UserListResponse.java`
- `sofit-admin/.../dev/dto/response/UserItemResponse.java`
- `sofit-admin/.../dev/repository/UserSpecification.java`
- `sofit-admin/.../dev/converter/DevUserConverter.java`
- `sofit-admin/.../dev/service/DevUserService.java`
- `sofit-admin/.../dev/service/DevUserServiceImpl.java`
- `sofit-admin/.../dev/controller/DevUserControllerDocs.java`
- `sofit-admin/.../dev/controller/DevUserController.java`

### 수정
- `sofit-common/.../repository/user/UserRepository.java` (JpaSpecificationExecutor 추가)
- `sofit-admin/.../global/config/SecurityConfig.java` (/api/admin/users/** 접근 제어 추가)
