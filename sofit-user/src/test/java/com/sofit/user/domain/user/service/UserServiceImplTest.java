package com.sofit.user.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import com.sofit.user.domain.user.event.UserWithdrawnEvent;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserServiceImpl userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    // ===== findUser 테스트 =====

    @Nested
    @DisplayName("findUser")
    class FindUserTest {

        @Test
        @DisplayName("활성 사용자 조회 시 UserProfileResponse를 반환한다")
        void findUser_활성_사용자_조회시_응답_반환() {
            // given
            Long userId = 1L;
            User user = createActiveUser(userId, "testuser", "홍길동");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            UserProfileResponse response = userService.findUser(userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.name()).isEqualTo("홍길동");
            assertThat(response.loginId()).isEqualTo("testuser");
            assertThat(response.phoneNumber()).isEqualTo("01012345678");
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 시 USER_NOT_FOUND 예외가 발생한다")
        void findUser_존재하지_않는_사용자_USER_NOT_FOUND_예외_발생() {
            // given
            Long userId = 99L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.findUser(userId))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.USER_NOT_FOUND));
        }

        @Test
        @DisplayName("탈퇴 계정 조회 시 ACCOUNT_WITHDRAWN 예외가 발생한다")
        void findUser_탈퇴_계정_조회시_ACCOUNT_WITHDRAWN_예외_발생() {
            // given
            Long userId = 2L;
            User inactiveUser = createActiveUser(userId, "withdrawn", "탈퇴자");
            inactiveUser.inactivate();

            given(userRepository.findById(userId)).willReturn(Optional.of(inactiveUser));

            // when & then
            assertThatThrownBy(() -> userService.findUser(userId))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.ACCOUNT_WITHDRAWN));
        }
    }

    // ===== withdraw 테스트 =====

    @Nested
    @DisplayName("withdraw")
    class WithdrawTest {

        @Test
        @DisplayName("정상 탈퇴 처리 시 사용자가 INACTIVE 상태로 변경되고 이벤트가 발행된다")
        void withdraw_정상_탈퇴_처리시_INACTIVE_변경_및_이벤트_발행() {
            // given
            Long userId = 1L;
            User user = createActiveUser(userId, "testuser", "홍길동");

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            userService.withdraw(userId);

            // then
            assertThat(user.getStatus().name()).isEqualTo("INACTIVE");
            assertThat(user.getInactivatedAt()).isNotNull();
            verify(eventPublisher).publishEvent(any(UserWithdrawnEvent.class));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 탈퇴 요청 시 USER_NOT_FOUND 예외가 발생한다")
        void withdraw_존재하지_않는_사용자_USER_NOT_FOUND_예외_발생() {
            // given
            Long userId = 99L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.withdraw(userId))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(AuthErrorCode.USER_NOT_FOUND));
        }
    }

    // ===== Helper Methods =====

    private User createActiveUser(Long userId, String loginId, String name) {
        User user = User.createUser(loginId, "hashedPassword", name, "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }
}
