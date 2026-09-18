package com.diet.util;

import com.diet.exception.DietException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class LlmJsonService {
    private final ObjectMapper objectMapper;

    public LlmJsonService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode parseObject(String content) {
        return parse(content, '{', '}');
    }

    public JsonNode parseArray(String content) {
        return parse(content, '[', ']');
    }

    private JsonNode parse(String content, char startChar, char endChar) {
        if (content == null || content.isBlank()) {
            throw new DietException("The LLM returned empty JSON content.");
        }
        String cleaned = content.strip()
                .replace("```json", "")
                .replace("```", "")
                .strip();
        int start = cleaned.indexOf(startChar);
        int end = cleaned.lastIndexOf(endChar);
        if (start < 0 || end < start) {
            throw new DietException("The LLM did not return valid JSON: " + content);
        }
        String json = cleaned.substring(start, end + 1);
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new DietException("Could not parse LLM JSON: " + json, e);
        }
    }
}
