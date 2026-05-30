package com.sofit.admin.domain.dev.converter;

import com.sofit.admin.domain.dev.dto.response.UserItemResponse;
import com.sofit.admin.domain.dev.dto.response.UserListResponse;
import com.sofit.common.entity.user.User;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * User Entity → 고객 정보 목록 조회 DTO 변환
 */
public class DevUserConverter {

    private DevUserConverter() {
    }

    public static UserItemResponse toUserItemResponse(User user) {
        return new UserItemResponse(
                user.getUserId(),
                user.getLoginId(),
                user.getName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getPhoneNumber(),
                user.getCreatedAt()
        );
    }

    public static UserListResponse toUserListResponse(Page<User> userPage, int currentPage, int size) {
        List<UserItemResponse> contents = userPage.getContent().stream()
                .map(DevUserConverter::toUserItemResponse)
                .toList();

        return new UserListResponse(
                contents,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                currentPage,
                size
        );
    }
}
