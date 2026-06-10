package com.sofit.user.domain.user.event;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;

/**
 * UserWithdrawnEventListener 단위 테스트.
 * DB 커밋 후 해당 사용자의 Redis 세션을 삭제하는 로직을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserWithdrawnEventListenerTest {

    @InjectMocks
    private UserWithdrawnEventListener userWithdrawnEventListener;

    @SuppressWarnings("rawtypes")
    @Mock
    private FindByIndexNameSessionRepository sessionRepository;

    // ===== handleUserWithdrawn 테스트 =====

    @Nested
    @DisplayName("handleUserWithdrawn")
    class HandleUserWithdrawnTest {

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("활성 세션이 있으면 해당 세션을 모두 삭제한다")
        void handleUserWithdrawn_활성_세션_있으면_모두_삭제() {
            // given
            Long userId = 1L;
            UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

            Session session1 = org.mockito.Mockito.mock(Session.class);
            Session session2 = org.mockito.Mockito.mock(Session.class);
            Map<String, Session> sessions = new HashMap<>();
            sessions.put("session-id-1", session1);
            sessions.put("session-id-2", session2);

            given(sessionRepository.findByPrincipalName("1")).willReturn(sessions);

            // when
            userWithdrawnEventListener.handleUserWithdrawn(event);

            // then
            verify(sessionRepository).deleteById("session-id-1");
            verify(sessionRepository).deleteById("session-id-2");
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("활성 세션이 없으면 삭제 호출이 발생하지 않는다")
        void handleUserWithdrawn_활성_세션_없으면_삭제_호출_없음() {
            // given
            Long userId = 2L;
            UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

            given(sessionRepository.findByPrincipalName("2")).willReturn(new HashMap<>());

            // when
            userWithdrawnEventListener.handleUserWithdrawn(event);

            // then
            verify(sessionRepository, never()).deleteById(org.mockito.ArgumentMatchers.anyString());
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("Redis 장애 시 예외가 전파되지 않고 로그만 남긴다")
        void handleUserWithdrawn_Redis_장애시_예외_전파_안함() {
            // given
            Long userId = 3L;
            UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

            given(sessionRepository.findByPrincipalName("3"))
                    .willThrow(new RuntimeException("Redis connection failed"));

            // when & then — 예외가 던져지지 않아야 함
            org.assertj.core.api.Assertions.assertThatCode(
                    () -> userWithdrawnEventListener.handleUserWithdrawn(event)
            ).doesNotThrowAnyException();
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("단일 세션만 있을 때 해당 세션 하나만 삭제한다")
        void handleUserWithdrawn_단일_세션_있으면_해당_세션만_삭제() {
            // given
            Long userId = 4L;
            UserWithdrawnEvent event = new UserWithdrawnEvent(userId);

            Session session = org.mockito.Mockito.mock(Session.class);
            Map<String, Session> sessions = new HashMap<>();
            sessions.put("session-id-only", session);

            given(sessionRepository.findByPrincipalName("4")).willReturn(sessions);

            // when
            userWithdrawnEventListener.handleUserWithdrawn(event);

            // then
            verify(sessionRepository).deleteById("session-id-only");
        }
    }
}
