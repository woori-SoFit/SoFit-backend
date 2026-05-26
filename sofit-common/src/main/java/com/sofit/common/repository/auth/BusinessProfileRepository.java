package com.sofit.common.repository.auth;

import com.sofit.common.entity.auth.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {

    Optional<BusinessProfile> findByBusinessNumber(String businessNumber);

    Optional<BusinessProfile> findByUser_UserId(Long userId);

    List<BusinessProfile> findByUser_UserIdIn(List<Long> userIds);
}
