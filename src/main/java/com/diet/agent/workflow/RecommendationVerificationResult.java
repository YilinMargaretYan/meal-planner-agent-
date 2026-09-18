package com.diet.agent.workflow;

import com.diet.model.RecommendResult;
import com.diet.model.ResponseResult;

import java.util.List;

/** Final deterministic verification result, including any repaired safe response. */
public record RecommendationVerificationResult(
        boolean passed,
        boolean repaired,
        List<String> violations,
        RecommendResult recommend,
        ResponseResult response
) {
    public RecommendationVerificationResult {
        violations = violations == null ? List.of() : List.copyOf(violations);
    }
}
