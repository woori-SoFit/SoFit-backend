package com.sofit.user.domain.sgrade.service;

public interface SGradeService {

    /**
     * 비동기로 S등급 산출을 수행한다.
     * 회원가입 완료 후 백그라운드에서 실행된다.
     *
     * @param userId 사용자 ID
     * @param sGradeId s_grade_history의 PK
     */
    void predictAsync(Long userId, Long sGradeId);
}
