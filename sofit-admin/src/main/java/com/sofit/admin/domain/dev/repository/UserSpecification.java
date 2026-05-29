package com.sofit.admin.domain.dev.repository;

import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;
import com.sofit.common.entity.user.enums.UserStatus;
import org.springframework.data.jpa.domain.Specification;

/**
 * 고객 정보 목록 조회 시 동적 검색 조건을 위한 Specification
 */
public class UserSpecification {

    private UserSpecification() {
    }

    /**
     * 이름 또는 로그인 아이디 부분 매칭 (LIKE)
     */
    public static Specification<User> keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword + "%";
        return (root, query, cb) -> cb.or(
                cb.like(root.get("name"), pattern),
                cb.like(root.get("loginId"), pattern)
        );
    }

    /**
     * 역할 필터
     */
    public static Specification<User> roleEquals(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("role"), UserRole.valueOf(role));
    }

    /**
     * 상태 필터
     */
    public static Specification<User> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), UserStatus.valueOf(status));
    }
}
