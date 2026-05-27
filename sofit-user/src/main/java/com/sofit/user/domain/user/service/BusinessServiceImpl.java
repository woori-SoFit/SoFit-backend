package com.sofit.user.domain.user.service;

import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.user.domain.user.converter.BusinessConverter;
import com.sofit.user.domain.user.dto.response.BusinessProfileResponse;
import com.sofit.user.domain.user.exception.BusinessErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BusinessServiceImpl implements BusinessService {

    private final BusinessProfileRepository businessProfileRepository;

    @Override
    public BusinessProfileResponse findBusinessProfile(Long userId) {
        // 1. 사업자 프로필 조회 (미존재 시 BUSINESS_PROFILE_NOT_FOUND 예외)
        BusinessProfile businessProfile = businessProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BaseException(BusinessErrorCode.BUSINESS_PROFILE_NOT_FOUND));

        // 2. Entity → DTO 변환 후 반환
        return BusinessConverter.toBusinessProfileResponse(businessProfile);
    }

    @Transactional
    @Override
    public void connectMybiz(Long userId) {
        BusinessProfile profile = businessProfileRepository.findByUser_UserId(userId)
                .orElseThrow(() -> new BaseException(BusinessErrorCode.BUSINESS_PROFILE_NOT_FOUND));
        profile.connectMybiz();
    }
}
