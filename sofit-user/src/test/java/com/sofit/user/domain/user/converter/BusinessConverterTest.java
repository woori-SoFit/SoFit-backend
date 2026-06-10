package com.sofit.user.domain.user.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.user.User;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;

class BusinessConverterTest {

    @Test
    @DisplayName("BusinessProfile 엔티티를 BusinessProfileResponse로 변환한다")
    void toBusinessProfileResponse_정상_변환() {
        // given
        User user = User.createUser("testuser", "hashedPw", "홍길동", "01012345678", "9001011");
        ReflectionTestUtils.setField(user, "userId", 1L);

        BusinessProfile profile = BusinessProfile.createVerified(
                user, "1234567890", "홍길동", "한식", "음식점업",
                "맛있는식당", "서울시 강남구", LocalDate.of(2020, 3, 15)
        );

        // when
        BusinessProfileResponse response = BusinessConverter.toBusinessProfileResponse(profile);

        // then
        assertThat(response.businessNumber()).isEqualTo("1234567890");
        assertThat(response.businessName()).isEqualTo("맛있는식당");
        assertThat(response.representativeName()).isEqualTo("홍길동");
        assertThat(response.residentNumber()).isEqualTo("9001011");
        assertThat(response.openDate()).isEqualTo(LocalDate.of(2020, 3, 15));
        assertThat(response.businessCategory()).isEqualTo("한식");
        assertThat(response.businessType()).isEqualTo("음식점업");
        assertThat(response.businessAddress()).isEqualTo("서울시 강남구");
        assertThat(response.isMybizConnected()).isFalse();
    }

    @Test
    @DisplayName("MyBiz 연동된 BusinessProfile을 변환하면 isMybizConnected가 true이다")
    void toBusinessProfileResponse_MyBiz_연동_상태_변환() {
        // given
        User user = User.createUser("bizuser", "pw", "김사장", "01099998888", "8501012");
        ReflectionTestUtils.setField(user, "userId", 2L);

        BusinessProfile profile = BusinessProfile.createVerified(
                user, "9876543210", "김사장", "소매업", "편의점",
                "김사장편의점", "부산시 해운대구", LocalDate.of(2018, 7, 1)
        );
        profile.connectMybiz();

        // when
        BusinessProfileResponse response = BusinessConverter.toBusinessProfileResponse(profile);

        // then
        assertThat(response.businessNumber()).isEqualTo("9876543210");
        assertThat(response.isMybizConnected()).isTrue();
    }

    @Test
    @DisplayName("openDate가 null인 BusinessProfile을 변환한다")
    void toBusinessProfileResponse_openDate_null_변환() {
        // given
        User user = User.createUser("nodate", "pw", "박대표", "01055556666", "9201013");
        ReflectionTestUtils.setField(user, "userId", 3L);

        BusinessProfile profile = BusinessProfile.createVerified(
                user, "1111111111", "박대표", "IT", "소프트웨어개발",
                "박대표회사", "대전시 유성구", null
        );

        // when
        BusinessProfileResponse response = BusinessConverter.toBusinessProfileResponse(profile);

        // then
        assertThat(response.openDate()).isNull();
        assertThat(response.businessName()).isEqualTo("박대표회사");
    }
}
