package com.sofit.admin.global.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.enums.UserRole;

/**
 * AdminRoleService 단위 테스트.
 * SecurityContext에서 현재 사용자의 역할을 올바르게 반환하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AdminRoleServiceTest {

    @InjectMocks
    private AdminRoleService adminRoleService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ===== getCurrentUserRole 테스트 =====

    @Nested
    @DisplayName("getCurrentUserRole")
    class GetCurrentUserRoleTest {

        @Test
        @DisplayName("ADMIN_BANK_TELLER 권한으로 인증된 경우 ADMIN_BANK_TELLER를 반환한다")
        void getCurrentUserRole_ADMIN_BANK_TELLER_권한이면_ADMIN_BANK_TELLER_반환() {
            // given
            setAuthentication(1L, "ADMIN_BANK_TELLER");

            // when
            UserRole result = adminRoleService.getCurrentUserRole();

            // then
            assertThat(result).isEqualTo(UserRole.ADMIN_BANK_TELLER);
        }

        @Test
        @DisplayName("ADMIN_DEV 권한으로 인증된 경우 ADMIN_DEV를 반환한다")
        void getCurrentUserRole_ADMIN_DEV_권한이면_ADMIN_DEV_반환() {
            // given
            setAuthentication(2L, "ADMIN_DEV");

            // when
            UserRole result = adminRoleService.getCurrentUserRole();

            // then
            assertThat(result).isEqualTo(UserRole.ADMIN_DEV);
        }

        @Test
        @DisplayName("인증 정보가 없으면 SESSION_EXPIRED 예외가 발생한다")
        void getCurrentUserRole_인증정보_없으면_SESSION_EXPIRED_예외_발생() {
            // given — SecurityContext 명시적으로 비우기
            SecurityContextHolder.clearContext();

            // when & then
            assertThatThrownBy(() -> adminRoleService.getCurrentUserRole())
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("유효하지 않은 역할 문자열인 경우 SESSION_EXPIRED 예외가 발생한다")
        void getCurrentUserRole_유효하지않은_역할이면_SESSION_EXPIRED_예외_발생() {
            // given
            setAuthentication(1L, "INVALID_ROLE");

            // when & then
            assertThatThrownBy(() -> adminRoleService.getCurrentUserRole())
                    .isInstanceOf(BaseException.class)
                    .satisfies(ex -> assertThat(((BaseException) ex).getErrorCode().getCode())
                            .isEqualTo("AUTH4011"));
        }
    }

    // ===== Helper Methods =====

    private void setAuthentication(Long userId, String authority) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
