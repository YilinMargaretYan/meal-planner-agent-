package com.diet.service.meal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Small fail-open client for the OpenAI embeddings endpoint. */
@Service
public class OpenAiEmbeddingService {
    private static final Logger log = LoggerFactory.getLogger(OpenAiEmbeddingService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public OpenAiEmbeddingService(
            ObjectMapper objectMapper,
            @Value("${agentscope.openai.api-key}") String apiKey,
            @Value("${diet.rag.embedding.model:text-embedding-3-small}") String model,
            @Value("${diet.rag.embedding.api-base-url:https://api.openai.com/v1}") String apiBaseUrl) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder().baseUrl(apiBaseUrl).build();
    }

    public String model() {
        return model;
    }

    /** Returns no vectors on a provider error so recommendation retrieval can continue safely. */
    public List<List<Double>> embed(List<String> inputs) {
        if (inputs == null || inputs.isEmpty() || inputs.stream().anyMatch(text -> text == null || text.isBlank())) {
            return List.of();
        }
        try {
            JsonNode response = restClient.post()
                    .uri("/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", model, "input", inputs, "encoding_format", "float"))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.path("data").isArray()) {
                return List.of();
            }
            List<List<Double>> vectors = new ArrayList<>();
            for (JsonNode item : response.path("data")) {
                List<Double> vector = objectMapper.convertValue(
                        item.path("embedding"), objectMapper.getTypeFactory().constructCollectionType(List.class, Double.class));
                vectors.add(vector);
            }
            return vectors.size() == inputs.size() ? vectors : List.of();
        } catch (RuntimeException error) {
            log.warn("Embedding request failed; continuing with tag retrieval: {}", error.getClass().getSimpleName());
            return List.of();
        }
    }
}
