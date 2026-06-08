package com.sofit.admin.domain.auth.converter;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
import com.sofit.common.entity.user.User;
import com.sofit.common.entity.user.enums.UserRole;

class AdminAuthConverterTest {

    @Test
    @DisplayName("User 엔티티를 AdminLoginResponse로 변환한다")
    void toLoginResponse_정상_변환() {
        // given
        User user = createAdminUser(1L, "admin1", "관리자", "01099998888", UserRole.ADMIN_BANK_TELLER);

        // when
        AdminLoginResponse response = AdminAuthConverter.toLoginResponse(user);

        // then
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("관리자");
        assertThat(response.role()).isEqualTo("ADMIN_BANK_TELLER");
    }

    @Test
    @DisplayName("DEV_ADMIN 역할 User를 AdminLoginResponse로 변환한다")
    void toLoginResponse_DEV_ADMIN_역할_변환() {
        // given
        User user = createAdminUser(2L, "devadmin", "개발관리자", "01011112222", UserRole.ADMIN_DEV);

        // when
        AdminLoginResponse response = AdminAuthConverter.toLoginResponse(user);

        // then
        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.name()).isEqualTo("개발관리자");
        assertThat(response.role()).isEqualTo("ADMIN_DEV");
    }

    @Test
    @DisplayName("User 엔티티를 AdminMeResponse로 변환한다")
    void toMeResponse_정상_변환() {
        // given
        User user = createAdminUser(3L, "bankadmin", "은행원", "01033334444", UserRole.ADMIN_BANK_TELLER);

        // when
        AdminMeResponse response = AdminAuthConverter.toMeResponse(user);

        // then
        assertThat(response.userId()).isEqualTo(3L);
        assertThat(response.name()).isEqualTo("은행원");
        assertThat(response.loginId()).isEqualTo("bankadmin");
        assertThat(response.phoneNumber()).isEqualTo("01033334444");
        assertThat(response.role()).isEqualTo(UserRole.ADMIN_BANK_TELLER);
    }

    private User createAdminUser(Long userId, String loginId, String name, String phoneNumber, UserRole role) {
        User user = User.createUser(loginId, "hashedPw", name, phoneNumber, "9001011");
        ReflectionTestUtils.setField(user, "userId", userId);
        ReflectionTestUtils.setField(user, "role", role);
        return user;
    }
}
