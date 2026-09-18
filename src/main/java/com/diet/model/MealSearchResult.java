package com.diet.model;

import java.util.List;

/** Result of a strict retrieval pass optionally expanded by source-safe fallback recall. */
public record MealSearchResult(
        List<MealItem> candidates,
        int strictCandidateCount,
        boolean fallbackRecallUsed,
        int semanticCandidateCount,
        boolean semanticRecallUsed
) {
    public MealSearchResult {
        candidates = candidates == null ? List.of() : List.copyOf(candidates);
    }
}
