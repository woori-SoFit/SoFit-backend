package com.sofit.user.domain.auth.service;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.apiPayload.BaseException;
import com.sofit.common.entity.auth.BusinessProfile;
import com.sofit.common.entity.user.User;
import com.sofit.common.repository.auth.BusinessProfileRepository;
import com.sofit.common.repository.user.UserRepository;
import com.sofit.user.domain.auth.dto.request.BusinessVerificationRequest;
import com.sofit.user.domain.auth.dto.response.BusinessVerificationResponse;
import com.sofit.user.domain.auth.dto.response.ExternalKycResponse;
import com.sofit.user.domain.auth.exception.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final ExternalMockClient externalMockClient;
    private final BusinessProfileRepository businessProfileRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public BusinessVerificationResponse verifyBusiness(BusinessVerificationRequest request, Long userId) {
        // 1. External Mock 서버에 KYC 인증 요청
        ApiResponse<ExternalKycResponse> mockResponse = externalMockClient.callKycVerify(request.businessNumber());

        // 2. 유효하지 않은 사업자 (폐업/미등록) 처리
        if (!mockResponse.isSuccess() || mockResponse.getResult() == null || !mockResponse.getResult().isValid()) {
            throw new BaseException(AuthErrorCode.BUSINESS_NOT_FOUND);
        }

        ExternalKycResponse kycResult = mockResponse.getResult();

        // 3. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BaseException(AuthErrorCode.BUSINESS_NOT_FOUND));

        // 4. BusinessProfile 저장
        BusinessProfile profile = BusinessProfile.createVerified(
                user,
                kycResult.businessNumber(),
                kycResult.representativeName(),
                kycResult.businessCategory(),
                kycResult.businessType(),
                kycResult.businessName(),
                kycResult.businessAddress(),
                LocalDate.parse(kycResult.openDate())
        );

        BusinessProfile saved = businessProfileRepository.save(profile);

        // 5. 응답 반환
        return new BusinessVerificationResponse(
                saved.getId(),
                saved.getBusinessNumber(),
                saved.getRepresentativeName(),
                saved.getBusinessName(),
                saved.getBusinessType(),
                kycResult.openDate(),
                true,
                saved.getVerifiedAt()
        );
    }
}
