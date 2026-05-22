package com.sofit.user.domain.user.service;

import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

    UserProfileResponse findUser(Long userId);

    void withdraw(Long userId, HttpServletRequest request);
}
