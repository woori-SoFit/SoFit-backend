package com.sofit.user.domain.user.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.entity.user.User;
import com.sofit.user.domain.user.dto.response.UserProfileResponse;

class UserConverterTest {

    @Test
    @DisplayName("User 엔티티를 UserProfileResponse로 변환한다")
    void toUserProfileResponse_정상_변환() {
        // given
        User user = User.createUser("testuser1", "hashedPw", "홍길동", "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", 1L);

        // when
        UserProfileResponse response = UserConverter.toUserProfileResponse(user);

        // then
        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.loginId()).isEqualTo("testuser1");
        assertThat(response.phoneNumber()).isEqualTo("01012345678");
        assertThat(response.residentNumber()).isEqualTo("9001011");
    }

    @Test
    @DisplayName("다른 사용자 정보로 UserProfileResponse 변환을 확인한다")
    void toUserProfileResponse_다른_사용자_변환() {
        // given
        User user = User.createUser("kimuser", "pw123", "김철수", "01099887766", "0503012");
        ReflectionTestUtils.setField(user, "userId", 99L);

        // when
        UserProfileResponse response = UserConverter.toUserProfileResponse(user);

        // then
        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.loginId()).isEqualTo("kimuser");
        assertThat(response.phoneNumber()).isEqualTo("01099887766");
        assertThat(response.residentNumber()).isEqualTo("0503012");
    }
}
