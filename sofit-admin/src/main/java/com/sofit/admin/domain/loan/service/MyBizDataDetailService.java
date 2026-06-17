package com.sofit.admin.domain.loan.service;

import com.sofit.admin.domain.loan.dto.response.MyBizDataDetailResponse;

public interface MyBizDataDetailService {

    MyBizDataDetailResponse findMyBizDataDetail(Long applicationId);
}
