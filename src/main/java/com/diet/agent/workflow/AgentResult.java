package com.diet.agent.workflow;

import java.util.Objects;

/** Typed envelope returned by every specialist worker. */
public record AgentResult<T>(
        String workerName,
        AgentExecutionStatus status,
        T data,
        double confidence,
        long latencyMs,
        String fallbackReason
) {
    public AgentResult {
        workerName = Objects.requireNonNull(workerName, "workerName cannot be null");
        status = Objects.requireNonNull(status, "status cannot be null");
        data = Objects.requireNonNull(data, "data cannot be null");
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        latencyMs = Math.max(0L, latencyMs);
    }
}
