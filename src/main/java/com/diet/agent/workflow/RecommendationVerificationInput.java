package com.diet.agent.workflow;

import com.diet.model.MealItem;
import com.diet.model.RecommendResult;
import com.diet.model.ResponseResult;

import java.util.List;

/** Inputs required to validate a user-facing recommendation before it is returned. */
public record RecommendationVerificationInput(
        List<MealItem> rankedMeals,
        RecommendResult recommend,
        ResponseResult response
) {
    public RecommendationVerificationInput {
        rankedMeals = rankedMeals == null ? List.of() : List.copyOf(rankedMeals);
    }
}
