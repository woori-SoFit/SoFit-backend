package com.sofit.user.domain.user.event;

/**
 * 회원탈퇴 DB 커밋 완료 후 세션 정리를 위한 이벤트.
 */
public record UserWithdrawnEvent(
        Long userId
) {
}
