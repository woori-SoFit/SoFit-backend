package com.sofit.user.domain.mybiz.service;

import com.sofit.user.domain.mybiz.dto.response.MyBizDashboardResponse;

public interface MyBizService {

    MyBizDashboardResponse findDashboard(Long userId, String month);
}
