package com.sofit.common.entity.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DB의 JSON 객체 문자열({"키": 값})을 LinkedHashMap<String, Double>로 변환하는 JPA Converter.
 * 원본 순서를 유지한다.
 */
@Converter
public class StringDoubleMapConverter implements AttributeConverter<Map<String, Double>, String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, Double> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    @Override
    public Map<String, Double> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(dbData, new TypeReference<LinkedHashMap<String, Double>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyMap();
        }
    }
}
