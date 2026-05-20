package com.sofit.user.domain.user.service;

import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import jakarta.servlet.http.HttpSession;

public interface UserService {

    UserProfileResponse findUser(HttpSession session);
}
