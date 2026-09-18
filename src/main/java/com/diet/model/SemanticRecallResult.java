package com.diet.model;

import java.util.List;

/** Semantic candidates produced from the already source-scoped meal set. */
public record SemanticRecallResult(List<MealItem> candidates, boolean used) {
    public SemanticRecallResult {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }

    public static SemanticRecallResult unavailable() {
        return new SemanticRecallResult(List.of(), false);
    }
}
