package com.sofit.user.domain.terms.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.user.domain.terms.dto.request.ConsentCreateRequest;
import com.sofit.user.domain.terms.dto.response.ConsentCreateResponse;
import com.sofit.user.domain.terms.exception.TermSuccessCode;
import com.sofit.user.domain.terms.service.TermService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermConsentController implements TermConsentControllerDocs {

    private final TermService termService;

    @Override
    @PostMapping("/consents")
    public ApiResponse<ConsentCreateResponse> createConsents(
            @Valid @RequestBody ConsentCreateRequest request,
            HttpSession session) {
        ConsentCreateResponse response = termService.createConsents(session, request);
        return ApiResponse.onSuccess(TermSuccessCode.CONSENT_OK, response);
    }
}
