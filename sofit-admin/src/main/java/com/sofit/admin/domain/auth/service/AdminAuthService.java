package com.sofit.admin.domain.auth.service;

import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import com.sofit.admin.domain.auth.dto.response.AdminMeResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    AdminMeResponse findMe();

    void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse);
}
