package com.sofit.user.domain.user.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * 회원탈퇴 트랜잭션 커밋 완료 후 Redis 세션을 삭제하는 리스너.
 * DB 커밋이 성공한 이후에만 세션이 삭제되므로 데이터 불일치를 방지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserWithdrawnEventListener {

    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserWithdrawn(UserWithdrawnEvent event) {
        try {
            // 해당 사용자의 모든 활성 세션 삭제 (Redis에서 역조회)
            Map<String, ? extends Session> userSessions =
                    sessionRepository.findByPrincipalName(event.userId().toString());
            userSessions.keySet().forEach(sessionRepository::deleteById);

            log.info("[회원탈퇴] userId={} 세션 {}개 삭제 완료", event.userId(), userSessions.size());
        } catch (Exception e) {
            // Redis 장애 시에도 DB 탈퇴 처리는 이미 완료된 상태
            // 세션은 Redis TTL(30분)로 자연 만료되므로 로그만 남기고 진행
            log.warn("[회원탈퇴] userId={} Redis 세션 삭제 실패 — TTL 만료 대기", event.userId(), e);
        }
    }
}
