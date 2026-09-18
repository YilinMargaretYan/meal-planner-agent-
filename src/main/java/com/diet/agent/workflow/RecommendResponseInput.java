package com.diet.agent.workflow;

import com.diet.model.MealItem;

import java.util.List;

/** Ranked candidates supplied to the recommendation-response specialist. */
public record RecommendResponseInput(List<MealItem> rankedMeals) {
    public RecommendResponseInput {
        rankedMeals = rankedMeals == null ? List.of() : List.copyOf(rankedMeals);
    }
}
