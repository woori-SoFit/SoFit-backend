package com.sofit.admin.global.util;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.user.enums.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SecurityUtil 단위 테스트")
class SecurityUtilTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getCurrentUserId")
    class GetCurrentUserIdTest {

        @Test
        @DisplayName("인증된 사용자의 userId를 반환한다")
        void shouldReturnUserId() {
            // given
            setAuthentication(1L, "ADMIN_DEV");

            // when
            Long userId = SecurityUtil.getCurrentUserId();

            // then
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("Authentication이 null이면 SESSION_EXPIRED 예외를 던진다")
        void shouldThrowWhenAuthenticationIsNull() {
            // given — SecurityContext 비어있음

            // when & then
            assertThatThrownBy(SecurityUtil::getCurrentUserId)
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("principal이 anonymousUser이면 SESSION_EXPIRED 예외를 던진다")
        void shouldThrowWhenAnonymous() {
            // given
            var auth = new UsernamePasswordAuthenticationToken(
                    "anonymousUser", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // when & then
            assertThatThrownBy(SecurityUtil::getCurrentUserId)
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("principal이 Long 타입이 아니면 SESSION_EXPIRED 예외를 던진다")
        void shouldThrowWhenPrincipalIsNotLong() {
            // given
            var auth = new UsernamePasswordAuthenticationToken(
                    "stringPrincipal", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // when & then
            assertThatThrownBy(SecurityUtil::getCurrentUserId)
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("isAuthenticated")
    class IsAuthenticatedTest {

        @Test
        @DisplayName("인증된 사용자면 true를 반환한다")
        void shouldReturnTrueWhenAuthenticated() {
            // given
            setAuthentication(1L, "ADMIN_DEV");

            // when & then
            assertThat(SecurityUtil.isAuthenticated()).isTrue();
        }

        @Test
        @DisplayName("Authentication이 null이면 false를 반환한다")
        void shouldReturnFalseWhenNull() {
            // when & then
            assertThat(SecurityUtil.isAuthenticated()).isFalse();
        }

        @Test
        @DisplayName("anonymousUser이면 false를 반환한다")
        void shouldReturnFalseWhenAnonymous() {
            // given
            var auth = new UsernamePasswordAuthenticationToken(
                    "anonymousUser", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // when & then
            assertThat(SecurityUtil.isAuthenticated()).isFalse();
        }
    }

    @Nested
    @DisplayName("getCurrentUserRole")
    class GetCurrentUserRoleTest {

        @Test
        @DisplayName("현재 사용자의 역할을 반환한다")
        void shouldReturnUserRole() {
            // given
            setAuthentication(1L, "ADMIN_BANK_TELLER");

            // when
            UserRole role = SecurityUtil.getCurrentUserRole();

            // then
            assertThat(role).isEqualTo(UserRole.ADMIN_BANK_TELLER);
        }

        @Test
        @DisplayName("Authentication이 null이면 SESSION_EXPIRED 예외를 던진다")
        void shouldThrowWhenAuthenticationIsNull() {
            // when & then
            assertThatThrownBy(SecurityUtil::getCurrentUserRole)
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("authorities가 비어있으면 SESSION_EXPIRED 예외를 던진다")
        void shouldThrowWhenAuthoritiesEmpty() {
            // given
            var auth = new UsernamePasswordAuthenticationToken(
                    1L, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // when & then
            assertThatThrownBy(SecurityUtil::getCurrentUserRole)
                    .isInstanceOf(BaseException.class);
        }

        @Test
        @DisplayName("유효하지 않은 authority이면 SESSION_EXPIRED 예외를 던진다")
        void shouldThrowWhenInvalidAuthority() {
            // given
            var auth = new UsernamePasswordAuthenticationToken(
                    1L, null, List.of(new SimpleGrantedAuthority("INVALID_ROLE")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            // when & then
            assertThatThrownBy(SecurityUtil::getCurrentUserRole)
                    .isInstanceOf(BaseException.class);
        }
    }

    @Nested
    @DisplayName("hasAuthority")
    class HasAuthorityTest {

        @Test
        @DisplayName("해당 권한을 보유하면 true를 반환한다")
        void shouldReturnTrueWhenHasAuthority() {
            // given
            setAuthentication(1L, "ADMIN_DEV");

            // when & then
            assertThat(SecurityUtil.hasAuthority("ADMIN_DEV")).isTrue();
        }

        @Test
        @DisplayName("해당 권한을 보유하지 않으면 false를 반환한다")
        void shouldReturnFalseWhenDoesNotHaveAuthority() {
            // given
            setAuthentication(1L, "ADMIN_DEV");

            // when & then
            assertThat(SecurityUtil.hasAuthority("ADMIN_BANK_MANAGER")).isFalse();
        }

        @Test
        @DisplayName("Authentication이 null이면 false를 반환한다")
        void shouldReturnFalseWhenAuthenticationIsNull() {
            // when & then
            assertThat(SecurityUtil.hasAuthority("ADMIN_DEV")).isFalse();
        }
    }

    // ===================== 헬퍼 =====================

    private void setAuthentication(Long userId, String authority) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority(authority)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
