package com.diet.service.risk;

import com.diet.enums.Intent;
import com.diet.model.RecommendResult;
import com.diet.model.ResponseResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskGuardServiceTest {

    private final RiskGuardService service = new RiskGuardService();

    @Test
    void allowsGeneralMealAdvice() {
        var result = service.check("I want a quick dinner", Intent.MEAL_RECOMMENDATION,
                RecommendResult.empty(), ResponseResult.textOnly("A warm bowl can be a practical dinner option."));

        assertTrue(result.passed());
    }

    @Test
    void blocksMedicalRiskLanguageInEnglish() {
        var result = service.check("What should I eat with diabetes?", Intent.MEAL_RECOMMENDATION,
                RecommendResult.empty(), ResponseResult.textOnly("Try a balanced meal."));

        assertFalse(result.passed());
        assertTrue(result.rewriteSuggestion().contains("medical"));
    }
}
