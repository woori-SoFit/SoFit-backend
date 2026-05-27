package com.sofit.user.domain.terms.controller;

import com.sofit.common.apiPayload.ApiResponse;
import com.sofit.common.entity.term.enums.TermType;
import com.sofit.user.domain.terms.dto.response.TermListResponse;
import com.sofit.user.domain.terms.exception.TermSuccessCode;
import com.sofit.user.domain.terms.service.TermService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/terms")
public class TermController implements TermControllerDocs {

    private final TermService termService;

    @Override
    @GetMapping
    public ApiResponse<TermListResponse> getTerms(@RequestParam TermType termType) {
        TermListResponse response = termService.findTerms(termType);
        return ApiResponse.onSuccess(TermSuccessCode.TERM_LIST_OK, response);
    }
}
