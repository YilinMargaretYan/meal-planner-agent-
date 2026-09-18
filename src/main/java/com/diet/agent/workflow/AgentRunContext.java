package com.diet.agent.workflow;

import com.diet.model.ConversationTurn;
import com.diet.model.SessionState;

import java.util.List;
import java.util.Objects;

/** Immutable, per-turn input shared by specialist workers. */
public record AgentRunContext(
        String traceId,
        String sessionId,
        Long userId,
        String userInput,
        AgentSessionSnapshot session,
        List<ConversationTurn> recentHistory,
        int llmCallBudget
) {
    public AgentRunContext {
        traceId = requireText(traceId, "traceId");
        sessionId = requireText(sessionId, "sessionId");
        userId = Objects.requireNonNull(userId, "userId cannot be null");
        userInput = requireText(userInput, "userInput");
        session = Objects.requireNonNull(session, "session cannot be null");
        recentHistory = recentHistory == null ? List.of() : List.copyOf(recentHistory);
        llmCallBudget = Math.max(0, llmCallBudget);
    }

    public static AgentRunContext forTurn(
            String traceId,
            String userInput,
            SessionState state,
            List<ConversationTurn> recentHistory,
            int llmCallBudget
    ) {
        AgentSessionSnapshot snapshot = AgentSessionSnapshot.from(state);
        return new AgentRunContext(
                traceId,
                snapshot.sessionId(),
                snapshot.userId(),
                userInput,
                snapshot,
                recentHistory,
                llmCallBudget
        );
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return value;
    }
}
