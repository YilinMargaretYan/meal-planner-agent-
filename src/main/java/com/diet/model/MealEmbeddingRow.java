package com.diet.model;

import lombok.Data;

/** Persistent semantic index entry. The meal row remains the source of truth. */
@Data
public class MealEmbeddingRow {
    private Long mealId;
    private String sourceType;
    private Long ownerUserId;
    private String contentHash;
    private String embeddingModel;
    private String embeddingJson;
}
