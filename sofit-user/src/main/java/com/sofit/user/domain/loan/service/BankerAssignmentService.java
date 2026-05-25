package com.sofit.user.domain.loan.service;

/**
 * 라운드로빈 방식으로 활성 은행원을 배정하는 서비스
 */
public interface BankerAssignmentService {

    /**
     * 라운드로빈 방식으로 활성 은행원을 배정하고 userId를 반환한다.
     *
     * @return 배정된 은행원의 userId
     * @throws com.sofit.common.apiPayload.BaseException NO_AVAILABLE_BANKER - 활성 은행원이 없는 경우
     */
    Long assignBanker();
}
