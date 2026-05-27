package com.sofit.user.domain.user.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import com.sofit.user.domain.user.exception.UserSuccessCode;
import com.sofit.user.domain.user.service.UserService;
import com.sofit.user.global.util.SecurityUtil;
import com.sofit.user.global.util.SessionUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> findUser() {
        if (!SecurityUtil.isAuthenticated()) {
            return ApiResponse.onSuccess(UserSuccessCode.USER_NOT_AUTHENTICATED, null);
        }
        Long userId = SecurityUtil.getCurrentUserId();
        UserProfileResponse response = userService.findUser(userId);
        return ApiResponse.onSuccess(UserSuccessCode.USER_PROFILE_OK, response);
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> withdraw(HttpServletRequest request, HttpServletResponse response) {
        Long userId = SecurityUtil.getCurrentUserId();
        userService.withdraw(userId);

        SessionUtil.invalidateSession(request, response);

        return ApiResponse.onSuccess(UserSuccessCode.WITHDRAW_SUCCESS, null);
    }
}
