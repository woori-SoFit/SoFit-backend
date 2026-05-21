package com.sofit.admin.domain.auth.service;

import com.sofit.admin.domain.auth.dto.request.AdminLoginRequest;
import com.sofit.admin.domain.auth.dto.response.AdminLoginResponse;
import jakarta.servlet.http.HttpSession;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest request, HttpSession session);
}
