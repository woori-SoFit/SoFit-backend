package com.sofit.user.domain.user.service;

import com.sofit.user.domain.user.dto.response.UserProfileResponse;

public interface UserService {

    UserProfileResponse findUser(Long userId);

    void withdraw(Long userId);
}
