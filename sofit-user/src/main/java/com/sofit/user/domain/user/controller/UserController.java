package com.sofit.user.domain.user.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.user.dto.response.UserProfileResponse;
import com.sofit.user.domain.user.exception.UserSuccessCode;
import com.sofit.user.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerDocs {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> findUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return ApiResponse.onSuccess(UserSuccessCode.USER_NOT_AUTHENTICATED, null);
        }
        UserProfileResponse response = userService.findUser(session);
        return ApiResponse.onSuccess(UserSuccessCode.USER_PROFILE_OK, response);
    }
}
