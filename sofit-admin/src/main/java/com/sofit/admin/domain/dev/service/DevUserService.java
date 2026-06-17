package com.sofit.admin.domain.dev.service;

import com.sofit.admin.domain.dev.dto.response.UserListResponse;
import com.sofit.admin.domain.dev.dto.response.UserStatisticsResponse;

public interface DevUserService {

    /**
     * 고객 정보 목록 조회 (페이징 + 필터)
     *
     * @param page    페이지 번호 (0부터 시작, null이면 기본값 0)
     * @param size    페이지당 건수 (null이면 기본값 8)
     * @param keyword 검색어 (이름, 아이디 부분 매칭)
     * @param role    역할 필터
     * @param status  상태 필터
     * @return 페이징된 사용자 목록
     */
    UserListResponse findUsers(Integer page, Integer size, String keyword, String role, String status);

    /**
     * 고객 정보 통계 조회
     *
     * @return 사용자 통계 (전체, 활성, 은행원, 고객, 비활성 수)
     */
    UserStatisticsResponse findUserStatistics();
}
