package com.diet.agent.workflow;

import com.diet.model.MealItem;
import com.diet.model.MealResponse;
import com.diet.model.RecommendResult;
import com.diet.model.RecommendedMealOption;
import com.diet.model.ResponseResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deterministic final gate for candidate grounding and health-disclaimer completeness. */
@Component
public class RecommendationVerifierWorker implements AgentWorker<RecommendationVerificationInput, RecommendationVerificationResult> {

    private static final String WORKER_NAME = "RecommendationVerifier";
    private static final String HEALTH_DISCLAIMER =
            "These are general meal suggestions. If you have a medical condition or special dietary needs, consult a clinician or registered dietitian.";
    private static final String VERIFICATION_FALLBACK =
            "The recommendations did not pass validation. Please try again or add a meal time, taste preference, or health goal.";

    @Override
    public AgentResult<RecommendationVerificationResult> execute(
            AgentRunContext context,
            RecommendationVerificationInput input
    ) {
        long startedAt = System.nanoTime();
        RecommendationVerificationResult verification = verify(input);
        boolean fallback = !verification.passed();
        return new AgentResult<>(
                WORKER_NAME,
                fallback ? AgentExecutionStatus.FALLBACK : AgentExecutionStatus.SUCCESS,
                verification,
                fallback ? 0.0 : 1.0,
                elapsedMs(startedAt),
                fallback ? String.join("; ", verification.violations()) : null
        );
    }

    private RecommendationVerificationResult verify(RecommendationVerificationInput input) {
        List<String> violations = new ArrayList<>();
        if (input == null || input.recommend() == null || input.response() == null) {
            violations.add("Recommendation or response was missing.");
            return reject(violations);
        }

        RecommendResult recommend = input.recommend();
        ResponseResult response = input.response();
        List<RecommendedMealOption> options = recommend.recommendations();
        if (options == null || options.isEmpty()) {
            violations.add("No recommendation options were returned.");
            return reject(violations);
        }

        Set<Long> rankedIds = new HashSet<>();
        for (MealItem meal : input.rankedMeals()) {
            if (meal != null && meal.id() != null) {
                rankedIds.add(meal.id());
            }
        }

        Set<Long> optionIds = new HashSet<>();
        for (RecommendedMealOption option : options) {
            if (option == null || option.itemId() == null || !rankedIds.contains(option.itemId())) {
                violations.add("Recommendation referenced a meal outside the ranked candidate set.");
                continue;
            }
            if (!optionIds.add(option.itemId())) {
                violations.add("Recommendation contained a duplicate meal.");
            }
        }

        Set<Long> cardIds = new HashSet<>();
        List<MealResponse> cards = response.displayBlocks();
        if (cards == null) {
            violations.add("Recommendation cards were missing.");
        } else {
            for (MealResponse card : cards) {
                if (card == null || card.id() == null) {
                    violations.add("Recommendation card was invalid.");
                } else {
                    cardIds.add(card.id());
                }
            }
            if (!cardIds.equals(optionIds)) {
                violations.add("Recommendation cards did not match the verified meal options.");
            }
        }

        if (response.speechText() == null || response.speechText().isBlank()) {
            violations.add("User-facing recommendation text was blank.");
        }
        if (!violations.isEmpty()) {
            return reject(violations);
        }

        if (recommend.needDisclaimer() && !response.speechText().contains("registered dietitian")) {
            ResponseResult repairedResponse = new ResponseResult(
                    response.speechText().stripTrailing() + "\n" + HEALTH_DISCLAIMER,
                    response.displayBlocks(),
                    response.nextAction()
            );
            return new RecommendationVerificationResult(true, true, List.of("Health disclaimer was added."), recommend, repairedResponse);
        }
        return new RecommendationVerificationResult(true, false, List.of(), recommend, response);
    }

    private RecommendationVerificationResult reject(List<String> violations) {
        return new RecommendationVerificationResult(
                false,
                true,
                violations,
                RecommendResult.empty(),
                ResponseResult.textOnly(VERIFICATION_FALLBACK)
        );
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
