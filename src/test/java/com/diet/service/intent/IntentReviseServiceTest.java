package com.diet.service.intent;

import com.diet.enums.Intent;
import com.diet.model.IntentResult;
import com.diet.model.SlotBundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentReviseServiceTest {

    private final IntentReviseService service = new IntentReviseService();

    @Test
    void routesEnglishHealthRiskKeywordToSafetyFlow() {
        IntentResult result = service.revise(null,
                new IntentResult(Intent.MEAL_RECOMMENDATION, SlotBundle.empty(), 0.9),
                "I have diabetes. What meal should I eat?");

        assertEquals(Intent.HEALTH_RISK, result.intent());
    }
}
