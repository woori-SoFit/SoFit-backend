package com.sofit.user.domain.sgrade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record SGradePredictResponse(
        @JsonProperty("s_grade") String sGrade,
        @JsonProperty("target_grade") String targetGrade,
        @JsonProperty("strength_keywords") List<String> strengthKeywords,
        @JsonProperty("improvement_keywords") List<String> improvementKeywords,
        @JsonProperty("strength_details") Map<String, Double> strengthDetails,
        @JsonProperty("improvement_details") Map<String, Double> improvementDetails,
        @JsonProperty("user_advice") String userAdvice,
        @JsonProperty("admin_advice") String adminAdvice
) {
}
