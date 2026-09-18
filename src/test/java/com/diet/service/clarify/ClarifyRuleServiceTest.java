package com.diet.service.clarify;

import com.diet.model.SlotBundle;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClarifyRuleServiceTest {

    private final ClarifyRuleService service = new ClarifyRuleService();

    @Test
    void asksForMealTimeAndGoalWhenNoDetailsAreKnown() {
        assertEquals(List.of("mealTime", "healthGoal"), service.missingSlots(SlotBundle.empty()));
    }

    @Test
    void asksForGoalWhenOnlyMealTimeIsKnown() {
        SlotBundle slots = new SlotBundle(List.of("Dinner"), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());

        assertEquals(List.of("healthGoal"), service.missingSlots(slots));
    }

    @Test
    void allowsRecommendationWhenMealTimeAndTasteAreKnown() {
        SlotBundle slots = new SlotBundle(List.of("Dinner"), List.of(), List.of(), List.of(), List.of(), List.of("Mild"), List.of());

        assertTrue(service.hasEnoughSlots(slots));
        assertFalse(service.fallbackQuestion(List.of("mealTime")).contains("早餐"));
    }
}
