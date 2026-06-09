package com.sofit.user.domain.sgrade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SGradePredictRequest(
        @JsonProperty("biz_data_id") Long bizDataId
) {
}
