package com.sofit.user.domain.user.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import com.sofit.user.domain.user.exception.BusinessSuccessCode;
import com.sofit.user.domain.user.service.BusinessService;
import com.sofit.user.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessController implements BusinessControllerDocs {

    private final BusinessService businessService;

    @Override
    @GetMapping("/me")
    public ApiResponse<BusinessProfileResponse> findBusinessProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        BusinessProfileResponse response = businessService.findBusinessProfile(userId);
        return ApiResponse.onSuccess(BusinessSuccessCode.BUSINESS_PROFILE_OK, response);
    }

    @Override
    @PostMapping("/me/mybiz-connect")
    public ApiResponse<Void> connectMybiz() {
        Long userId = SecurityUtil.getCurrentUserId();
        businessService.connectMybiz(userId);
        return ApiResponse.onSuccess(BusinessSuccessCode.MYBIZ_CONNECT_OK, null);
    }
}
