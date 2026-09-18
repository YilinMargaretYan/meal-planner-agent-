package com.diet.service.meal;

import com.diet.mapper.MealEmbeddingMapper;
import com.diet.model.MealEmbeddingRow;
import com.diet.model.MealItem;
import com.diet.model.SemanticRecallResult;
import com.diet.model.SlotBundle;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Semantic retrieval over the current source-scoped candidate set. This keeps
 * MySQL as the source of truth and prevents public/personal cross-retrieval.
 */
@Service
public class SemanticMealSearchService {
    private final MealEmbeddingMapper embeddingMapper;
    private final OpenAiEmbeddingService embeddingService;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int candidateLimit;

    public SemanticMealSearchService(
            MealEmbeddingMapper embeddingMapper,
            OpenAiEmbeddingService embeddingService,
            ObjectMapper objectMapper,
            @Value("${diet.rag.embedding.enabled:true}") boolean enabled,
            @Value("${diet.rag.embedding.candidate-limit:10}") int candidateLimit) {
        this.embeddingMapper = embeddingMapper;
        this.embeddingService = embeddingService;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.candidateLimit = Math.max(1, candidateLimit);
    }

    public SemanticRecallResult search(String userQuery, SlotBundle slots, List<MealItem> sourceScopedMeals) {
        if (!enabled || userQuery == null || userQuery.isBlank() || sourceScopedMeals == null || sourceScopedMeals.isEmpty()) {
            return SemanticRecallResult.unavailable();
        }
        try {
            Map<Long, List<Double>> documentVectors = ensureIndexed(sourceScopedMeals);
            List<List<Double>> queryVectors = embeddingService.embed(List.of(queryDocument(userQuery, slots)));
            if (documentVectors.isEmpty() || queryVectors.isEmpty()) {
                return SemanticRecallResult.unavailable();
            }
            List<Double> queryVector = queryVectors.get(0);
            return new SemanticRecallResult(sourceScopedMeals.stream()
                    .filter(item -> documentVectors.containsKey(item.id()))
                    .map(item -> withSemanticScore(item, cosine(queryVector, documentVectors.get(item.id()))))
                    .filter(item -> item.matchScore() > 0)
                    .sorted(Comparator.comparingDouble((MealItem item) -> item.matchScore()).reversed())
                    .limit(candidateLimit)
                    .toList(), true);
        } catch (RuntimeException ignored) {
            // A missing migration, unavailable provider, or malformed historical
            // row must never turn a recommendation request into a 500 response.
            return SemanticRecallResult.unavailable();
        }
    }

    /** Indexes a personal meal on write; provider failures are repaired lazily during retrieval. */
    public void index(MealItem meal) {
        if (!enabled || meal == null) {
            return;
        }
        try {
            ensureIndexed(List.of(meal));
        } catch (RuntimeException ignored) {
            // fail open: the row is still persisted and can be indexed later
        }
    }

    public void delete(Long mealId) {
        if (!enabled || mealId == null) {
            return;
        }
        try {
            embeddingMapper.deleteByMealId(mealId);
        } catch (RuntimeException ignored) {
            // A stale index entry cannot expose data because every search begins
            // with source-scoped meals read from meal_item.
        }
    }

    private Map<Long, List<Double>> ensureIndexed(List<MealItem> meals) {
        List<Long> mealIds = meals.stream().map(MealItem::id).toList();
        Map<Long, MealEmbeddingRow> existing = new HashMap<>();
        for (MealEmbeddingRow row : embeddingMapper.findByMealIds(mealIds)) {
            existing.put(row.getMealId(), row);
        }

        List<MealItem> staleMeals = meals.stream().filter(meal -> {
            MealEmbeddingRow row = existing.get(meal.id());
            return row == null || !embeddingService.model().equals(row.getEmbeddingModel())
                    || !contentHash(meal).equals(row.getContentHash());
        }).toList();
        if (!staleMeals.isEmpty()) {
            List<List<Double>> vectors = embeddingService.embed(staleMeals.stream().map(this::document).toList());
            if (vectors.size() == staleMeals.size()) {
                for (int index = 0; index < staleMeals.size(); index++) {
                    MealItem meal = staleMeals.get(index);
                    MealEmbeddingRow row = toEmbeddingRow(meal, vectors.get(index));
                    embeddingMapper.upsert(row);
                    existing.put(meal.id(), row);
                }
            }
        }

        Map<Long, List<Double>> vectors = new LinkedHashMap<>();
        for (MealItem meal : meals) {
            MealEmbeddingRow row = existing.get(meal.id());
            if (row != null) {
                List<Double> vector = readVector(row.getEmbeddingJson());
                if (!vector.isEmpty()) {
                    vectors.put(meal.id(), vector);
                }
            }
        }
        return vectors;
    }

    private MealEmbeddingRow toEmbeddingRow(MealItem meal, List<Double> vector) {
        MealEmbeddingRow row = new MealEmbeddingRow();
        row.setMealId(meal.id());
        row.setSourceType(meal.sourceType().name());
        row.setOwnerUserId(meal.ownerUserId());
        row.setContentHash(contentHash(meal));
        row.setEmbeddingModel(embeddingService.model());
        try {
            row.setEmbeddingJson(objectMapper.writeValueAsString(vector));
        } catch (Exception error) {
            throw new IllegalStateException("Unable to serialize embedding", error);
        }
        return row;
    }

    private List<Double> readVector(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() { });
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String document(MealItem meal) {
        SlotBundle slots = meal.slots() == null ? SlotBundle.empty() : meal.slots();
        return "Meal: " + meal.name()
                + "; meal time: " + String.join(", ", slots.mealTime())
                + "; mood: " + String.join(", ", slots.mood())
                + "; setting: " + String.join(", ", slots.scene())
                + "; health goal: " + String.join(", ", slots.healthGoal())
                + "; cuisine: " + String.join(", ", slots.cuisine())
                + "; taste: " + String.join(", ", slots.taste())
                + "; convenience: " + String.join(", ", slots.convenience());
    }

    private String queryDocument(String query, SlotBundle slots) {
        SlotBundle safeSlots = slots == null ? SlotBundle.empty() : slots;
        return "Meal request: " + query + "; recognized tags: "
                + String.join(", ", List.of(
                String.join(", ", safeSlots.mealTime()), String.join(", ", safeSlots.mood()),
                String.join(", ", safeSlots.scene()), String.join(", ", safeSlots.healthGoal()),
                String.join(", ", safeSlots.cuisine()), String.join(", ", safeSlots.taste()),
                String.join(", ", safeSlots.convenience())));
    }

    private MealItem withSemanticScore(MealItem item, double score) {
        return new MealItem(item.id(), item.sourceType(), item.ownerUserId(), item.name(), item.slots(), Math.max(0, score));
    }

    private double cosine(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || left.size() != right.size()) {
            return 0;
        }
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int index = 0; index < left.size(); index++) {
            double a = left.get(index);
            double b = right.get(index);
            dot += a * b;
            leftNorm += a * a;
            rightNorm += b * b;
        }
        return leftNorm == 0 || rightNorm == 0 ? 0 : dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private String contentHash(MealItem meal) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(document(meal).getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte value : digest) {
                hex.append(String.format(Locale.ROOT, "%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }
}
