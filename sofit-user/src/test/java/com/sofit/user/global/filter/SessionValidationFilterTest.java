package com.sofit.user.global.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDateTime;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SessionValidationFilter 단위 테스트.
 * 12시간 절대 만료 체크 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class SessionValidationFilterTest {

    @InjectMocks
    private SessionValidationFilter sessionValidationFilter;

    @Mock
    private ObjectMapper objectMapper;

    // ===== shouldNotFilter 테스트 =====

    @Nested
    @DisplayName("shouldNotFilter")
    class ShouldNotFilterTest {

        @Test
        @DisplayName("회원가입 경로는 필터를 건너뛴다")
        void shouldNotFilter_회원가입_경로는_필터_건너뜀() throws Exception {
            // given
            HttpServletRequest request = mockRequestWithUri("/api/auth/signup/business-verification");

            // when
            boolean result = sessionValidationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("로그인 경로는 필터를 건너뛴다")
        void shouldNotFilter_로그인_경로는_필터_건너뜀() throws Exception {
            // given
            HttpServletRequest request = mockRequestWithUri("/api/auth/login");

            // when
            boolean result = sessionValidationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("PIN 인증 경로는 필터를 건너뛴다")
        void shouldNotFilter_PIN_인증_경로는_필터_건너뜀() throws Exception {
            // given
            HttpServletRequest request = mockRequestWithUri("/api/auth/verify-pin");

            // when
            boolean result = sessionValidationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("내 정보 조회 경로는 필터를 건너뛴다")
        void shouldNotFilter_내정보_조회_경로는_필터_건너뜀() throws Exception {
            // given
            HttpServletRequest request = mockRequestWithUri("/api/users/me");

            // when
            boolean result = sessionValidationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Swagger 경로는 필터를 건너뛴다")
        void shouldNotFilter_Swagger_경로는_필터_건너뜀() throws Exception {
            // given
            HttpServletRequest request = mockRequestWithUri("/swagger-ui/index.html");

            // when
            boolean result = sessionValidationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("일반 API 경로는 필터를 적용한다")
        void shouldNotFilter_일반_API_경로는_필터_적용() throws Exception {
            // given
            HttpServletRequest request = mockRequestWithUri("/api/businesses/me");

            // when
            boolean result = sessionValidationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isFalse();
        }
    }

    // ===== doFilterInternal 테스트 =====

    @Nested
    @DisplayName("doFilterInternal")
    class DoFilterInternalTest {

        @Test
        @DisplayName("세션이 없으면 필터 체인을 그대로 통과한다")
        void doFilterInternal_세션_없으면_필터_체인_통과() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain filterChain = mock(FilterChain.class);

            given(request.getSession(false)).willReturn(null);

            // when
            sessionValidationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("세션은 있지만 loginTime 속성이 없으면 필터 체인을 통과한다")
        void doFilterInternal_세션있지만_loginTime없으면_필터_체인_통과() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain filterChain = mock(FilterChain.class);
            HttpSession session = mock(HttpSession.class);

            given(request.getSession(false)).willReturn(session);
            given(session.getAttribute("loginTime")).willReturn(null);

            // when
            sessionValidationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("loginTime이 12시간 이내이면 필터 체인을 통과한다")
        void doFilterInternal_loginTime_12시간_이내이면_필터_체인_통과() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain filterChain = mock(FilterChain.class);
            HttpSession session = mock(HttpSession.class);

            LocalDateTime recentLoginTime = LocalDateTime.now().minusHours(6); // 6시간 전 로그인

            given(request.getSession(false)).willReturn(session);
            given(session.getAttribute("loginTime")).willReturn(recentLoginTime);

            // when
            sessionValidationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(filterChain).doFilter(request, response);
            verify(session, never()).invalidate();
        }

        @Test
        @DisplayName("loginTime이 12시간 초과이면 세션을 무효화하고 401을 반환한다")
        void doFilterInternal_loginTime_12시간_초과이면_세션_무효화_401_반환() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain filterChain = mock(FilterChain.class);
            HttpSession session = mock(HttpSession.class);

            LocalDateTime expiredLoginTime = LocalDateTime.now().minusHours(13); // 13시간 전 로그인 (만료)

            given(request.getSession(false)).willReturn(session);
            given(session.getAttribute("loginTime")).willReturn(expiredLoginTime);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            given(response.getOutputStream())
                    .willReturn(new jakarta.servlet.ServletOutputStream() {
                        @Override
                        public boolean isReady() { return true; }
                        @Override
                        public void setWriteListener(jakarta.servlet.WriteListener l) {}
                        @Override
                        public void write(int b) { outputStream.write(b); }
                    });

            // when
            sessionValidationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(session).invalidate();
            verify(response).setStatus(401);
            verify(filterChain, never()).doFilter(any(), any());
        }

        @Test
        @DisplayName("loginTime이 정확히 12시간 이전이면 만료 처리한다")
        void doFilterInternal_loginTime_정확히_12시간_이전이면_만료_처리() throws Exception {
            // given
            HttpServletRequest request = mock(HttpServletRequest.class);
            HttpServletResponse response = mock(HttpServletResponse.class);
            FilterChain filterChain = mock(FilterChain.class);
            HttpSession session = mock(HttpSession.class);

            // 12시간 + 1초 전 로그인 → 만료
            LocalDateTime justExpired = LocalDateTime.now().minusHours(12).minusSeconds(1);

            given(request.getSession(false)).willReturn(session);
            given(session.getAttribute("loginTime")).willReturn(justExpired);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            given(response.getOutputStream())
                    .willReturn(new jakarta.servlet.ServletOutputStream() {
                        @Override
                        public boolean isReady() { return true; }
                        @Override
                        public void setWriteListener(jakarta.servlet.WriteListener l) {}
                        @Override
                        public void write(int b) { outputStream.write(b); }
                    });

            // when
            sessionValidationFilter.doFilterInternal(request, response, filterChain);

            // then
            verify(session).invalidate();
            verify(filterChain, never()).doFilter(any(), any());
        }
    }

    // ===== Helper Methods =====

    private HttpServletRequest mockRequestWithUri(String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getRequestURI()).willReturn(uri);
        return request;
    }
}
