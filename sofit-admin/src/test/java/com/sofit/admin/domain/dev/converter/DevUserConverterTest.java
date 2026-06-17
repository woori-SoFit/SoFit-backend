package com.sofit.admin.domain.dev.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.admin.domain.dev.dto.response.UserItemResponse;
import com.sofit.admin.domain.dev.dto.response.UserListResponse;
import com.sofit.common.entity.user.User;

class DevUserConverterTest {

    @Test
    @DisplayName("User 엔티티를 UserItemResponse로 변환한다")
    void toUserItemResponse_정상_변환() {
        // given
        User user = User.createUser("testuser", "hashedPw", "홍길동", "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", 1L);
        setBaseEntityField(user, "createdAt", LocalDateTime.of(2024, 6, 1, 10, 0));

        // when
        UserItemResponse response = DevUserConverter.toUserItemResponse(user);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.loginId()).isEqualTo("testuser");
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.role()).isEqualTo("USER");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.phoneNumber()).isEqualTo("01012345678");
        assertThat(response.createdAt()).isEqualTo(LocalDateTime.of(2024, 6, 1, 10, 0));
    }

    @Test
    @DisplayName("Page<User>를 UserListResponse로 변환한다")
    void toUserListResponse_정상_변환() {
        // given
        User user1 = User.createUser("user1", "pw1", "김철수", "01011111111", "9501011");
        ReflectionTestUtils.setField(user1, "userId", 1L);
        setBaseEntityField(user1, "createdAt", LocalDateTime.of(2024, 1, 1, 9, 0));

        User user2 = User.createUser("user2", "pw2", "이영희", "01022222222", "9601012");
        ReflectionTestUtils.setField(user2, "userId", 2L);
        setBaseEntityField(user2, "createdAt", LocalDateTime.of(2024, 2, 1, 9, 0));

        Page<User> userPage = new PageImpl<>(List.of(user1, user2), PageRequest.of(0, 10), 2);

        // when
        UserListResponse response = DevUserConverter.toUserListResponse(userPage, 0, 10);

        // then
        assertThat(response.contents()).hasSize(2);
        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.currentPage()).isZero();
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.contents().get(0).name()).isEqualTo("김철수");
        assertThat(response.contents().get(1).name()).isEqualTo("이영희");
    }

    @Test
    @DisplayName("빈 Page를 UserListResponse로 변환하면 빈 리스트를 반환한다")
    void toUserListResponse_빈_페이지_변환() {
        // given
        Page<User> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        // when
        UserListResponse response = DevUserConverter.toUserListResponse(emptyPage, 0, 10);

        // then
        assertThat(response.contents()).isEmpty();
        assertThat(response.totalCount()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    private void setBaseEntityField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getSuperclass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("BaseEntity 필드 설정 실패: " + fieldName, e);
        }
    }
}
