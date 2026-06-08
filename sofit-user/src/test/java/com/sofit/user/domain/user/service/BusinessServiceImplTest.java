package com.sofit.user.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import com.sofit.user.domain.user.exception.BusinessErrorCode;

@ExtendWith(MockitoExtension.class)
class BusinessServiceImplTest {

    @InjectMocks
    private BusinessServiceImpl businessService;

    @Mock
    private BusinessProfileRepository businessProfileRepository;

    // ===== findBusinessProfile 테스트 =====

    @Nested
    @DisplayName("findBusinessProfile")
    class FindBusinessProfileTest {

        @Test
        @DisplayName("사업자 프로필이 존재하면 BusinessProfileResponse를 반환한다")
        void findBusinessProfile_사업자_프로필_존재시_응답_반환() {
            // given
            Long userId = 1L;
            User user = createActiveUser(userId, "testuser", "홍길동");
            BusinessProfile profile = createBusinessProfile(user);

            given(businessProfileRepository.findByUser_UserId(userId))
                    .willReturn(Optional.of(profile));

            // when
            BusinessProfileResponse response = businessService.findBusinessProfile(userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.businessNumber()).isEqualTo("1234567890");
            assertThat(response.representativeName()).isEqualTo("홍길동");
            assertThat(response.businessName()).isEqualTo("테스트상호");
        }

        @Test
        @DisplayName("사업자 프로필이 없으면 BUSINESS_PROFILE_NOT_FOUND 예외가 발생한다")
        void findBusinessProfile_프로필_없으면_BUSINESS_PROFILE_NOT_FOUND_예외_발생() {
            // given
            Long userId = 99L;
            given(businessProfileRepository.findByUser_UserId(userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> businessService.findBusinessProfile(userId))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(BusinessErrorCode.BUSINESS_PROFILE_NOT_FOUND));
        }
    }

    // ===== connectMybiz 테스트 =====

    @Nested
    @DisplayName("connectMybiz")
    class ConnectMybizTest {

        @Test
        @DisplayName("사업자 프로필이 존재하면 마이비즈 연결 상태로 변경된다")
        void connectMybiz_프로필_존재시_마이비즈_연결_처리() {
            // given
            Long userId = 1L;
            User user = createActiveUser(userId, "testuser", "홍길동");
            BusinessProfile profile = createBusinessProfile(user);

            given(businessProfileRepository.findByUser_UserId(userId))
                    .willReturn(Optional.of(profile));

            // when
            businessService.connectMybiz(userId);

            // then
            verify(businessProfileRepository).findByUser_UserId(userId);
            assertThat(profile.isMybizConnected()).isTrue();
        }

        @Test
        @DisplayName("사업자 프로필이 없으면 BUSINESS_PROFILE_NOT_FOUND 예외가 발생한다")
        void connectMybiz_프로필_없으면_BUSINESS_PROFILE_NOT_FOUND_예외_발생() {
            // given
            Long userId = 99L;
            given(businessProfileRepository.findByUser_UserId(userId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> businessService.connectMybiz(userId))
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode())
                            .isEqualTo(BusinessErrorCode.BUSINESS_PROFILE_NOT_FOUND));
        }
    }

    // ===== Helper Methods =====

    private User createActiveUser(Long userId, String loginId, String name) {
        User user = User.createUser(loginId, "hashedPassword", name, "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private BusinessProfile createBusinessProfile(User user) {
        BusinessProfile profile = BusinessProfile.createVerified(
                user,
                "1234567890",
                "홍길동",
                "음식점업",
                "한식",
                "테스트상호",
                "서울시",
                LocalDate.of(2020, 1, 1)
        );
        return profile;
    }
}
