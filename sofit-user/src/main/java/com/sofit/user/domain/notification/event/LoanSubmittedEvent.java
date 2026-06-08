package com.sofit.user.domain.notification.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 대출 신청 완료 이벤트
 * - AFTER_COMMIT 이후 영속 컨텍스트가 닫히므로 엔티티 대신 ID만 전달
 */
@Getter
@AllArgsConstructor
public class LoanSubmittedEvent {

    private final Long userId;
    private final Long applicationId;
}
