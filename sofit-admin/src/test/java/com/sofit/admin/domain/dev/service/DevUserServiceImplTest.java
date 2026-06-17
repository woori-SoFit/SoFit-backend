package com.sofit.admin.domain.dev.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.admin.domain.dev.dto.response.UserListResponse;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.user.UserRepository;

@ExtendWith(MockitoExtension.class)
class DevUserServiceImplTest {

    @InjectMocks
    private DevUserServiceImpl devUserService;

    @Mock
    private UserRepository userRepository;

    // ===== findUsers 테스트 =====

    @Nested
    @DisplayName("findUsers")
    class FindUsersTest {

        @Test
        @DisplayName("기본 파라미터(null)로 조회 시 page=0, size=8로 처리한다")
        @SuppressWarnings("unchecked")
        void findUsers_기본_파라미터_null이면_page0_size8_처리() {
            // given
            List<User> users = List.of(createUser(1L, "user1", "홍길동"));
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 8), 1);

            given(userRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(userPage);

            // when
            UserListResponse response = devUserService.findUsers(null, null, null, null, null);

            // then
            assertThat(response).isNotNull();
            assertThat(response.contents()).hasSize(1);
            assertThat(response.totalCount()).isEqualTo(1L);
            assertThat(response.currentPage()).isEqualTo(0);
            assertThat(response.size()).isEqualTo(8);
        }

        @Test
        @DisplayName("page=1, size=5로 조회 시 해당 값으로 페이지 요청을 처리한다")
        @SuppressWarnings("unchecked")
        void findUsers_page1_size5로_조회시_해당_파라미터_처리() {
            // given
            List<User> users = List.of(createUser(2L, "user2", "김철수"));
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(1, 5), 10);

            given(userRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(userPage);

            // when
            UserListResponse response = devUserService.findUsers(1, 5, null, null, null);

            // then
            assertThat(response.currentPage()).isEqualTo(1);
            assertThat(response.size()).isEqualTo(5);
            assertThat(response.totalCount()).isEqualTo(10L);
            assertThat(response.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("결과가 없으면 빈 목록을 반환한다")
        @SuppressWarnings("unchecked")
        void findUsers_결과_없으면_빈_목록_반환() {
            // given
            Page<User> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 8), 0);

            given(userRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(emptyPage);

            // when
            UserListResponse response = devUserService.findUsers(null, null, null, null, null);

            // then
            assertThat(response.contents()).isEmpty();
            assertThat(response.totalCount()).isEqualTo(0L);
            assertThat(response.totalPages()).isEqualTo(0);
        }

        @Test
        @DisplayName("keyword, role, status 필터가 있어도 정상적으로 동작한다")
        @SuppressWarnings("unchecked")
        void findUsers_필터_파라미터_있어도_정상_동작() {
            // given
            List<User> users = List.of(createUser(1L, "user1", "홍길동"));
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 8), 1);

            given(userRepository.findAll(any(Specification.class), any(org.springframework.data.domain.Pageable.class)))
                    .willReturn(userPage);

            // when
            UserListResponse response = devUserService.findUsers(0, 8, "홍길동", "USER", "ACTIVE");

            // then
            assertThat(response).isNotNull();
            assertThat(response.contents()).hasSize(1);
        }
    }

    // ===== findUserStatistics 테스트 =====

    @Nested
    @DisplayName("findUserStatistics")
    class FindUserStatisticsTest {

        @Test
        @DisplayName("정상 조회 시 각 통계값을 올바르게 반환한다")
        void findUserStatistics_정상_조회시_통계값_반환() {
            // given
            given(userRepository.countByStatus(com.sofit.common.entity.user.enums.UserStatus.ACTIVE)).willReturn(80L);
            given(userRepository.countByStatus(com.sofit.common.entity.user.enums.UserStatus.INACTIVE)).willReturn(20L);
            given(userRepository.countByStatusAndRoleIn(
                    com.sofit.common.entity.user.enums.UserStatus.ACTIVE,
                    List.of(com.sofit.common.entity.user.enums.UserRole.ADMIN_DEV,
                            com.sofit.common.entity.user.enums.UserRole.ADMIN_BANK_TELLER,
                            com.sofit.common.entity.user.enums.UserRole.ADMIN_BANK_MANAGER)))
                    .willReturn(10L);
            given(userRepository.countByStatusAndRole(
                    com.sofit.common.entity.user.enums.UserStatus.ACTIVE,
                    com.sofit.common.entity.user.enums.UserRole.USER))
                    .willReturn(70L);

            // when
            var response = devUserService.findUserStatistics();

            // then
            assertThat(response.totalCount()).isEqualTo(100L);
            assertThat(response.activeCount()).isEqualTo(80L);
            assertThat(response.bankerCount()).isEqualTo(10L);
            assertThat(response.userCount()).isEqualTo(70L);
            assertThat(response.inactiveCount()).isEqualTo(20L);
        }
    }

    // ===== Helper Methods =====

    private User createUser(Long userId, String loginId, String name) {
        User user = User.createUser(loginId, "hashedPassword", name, "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }
}
