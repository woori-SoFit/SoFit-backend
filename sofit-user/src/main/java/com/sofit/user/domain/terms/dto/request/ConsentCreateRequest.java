package com.sofit.user.domain.terms.dto.request;

import java.util.List;

import com.sofit.common.entity.term.enums.TermType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ConsentCreateRequest {

    @NotNull
    private TermType termType;

    private Long applicationId;

    @NotEmpty
    @Valid
    private List<ConsentItem> consents;

    @Getter
    @NoArgsConstructor
    public static class ConsentItem {

        @NotNull
        private Long termId;

        @NotNull
        private Boolean isConsented;
    }
}
