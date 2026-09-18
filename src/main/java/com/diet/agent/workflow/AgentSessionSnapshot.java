package com.diet.agent.workflow;

import com.diet.enums.Intent;
import com.diet.enums.SessionPhase;
import com.diet.enums.SourceMode;
import com.diet.model.SessionState;
import com.diet.model.SlotBundle;

import java.util.List;
import java.util.Objects;

/** Immutable session view supplied to workers; workers cannot mutate orchestration state. */
public record AgentSessionSnapshot(
        String sessionId,
        Long userId,
        SessionPhase phase,
        SourceMode sourceMode,
        Intent currentIntent,
        SlotBundle slots,
        List<Long> lastRecommendations
) {
    public AgentSessionSnapshot {
        sessionId = Objects.requireNonNull(sessionId, "sessionId cannot be null");
        userId = Objects.requireNonNull(userId, "userId cannot be null");
        phase = Objects.requireNonNull(phase, "phase cannot be null");
        sourceMode = Objects.requireNonNull(sourceMode, "sourceMode cannot be null");
        slots = copySlots(slots);
        lastRecommendations = lastRecommendations == null ? List.of() : List.copyOf(lastRecommendations);
    }

    public static AgentSessionSnapshot from(SessionState state) {
        Objects.requireNonNull(state, "state cannot be null");
        return new AgentSessionSnapshot(
                state.sessionId(),
                state.userId(),
                state.phase(),
                state.sourceMode(),
                state.currentIntent(),
                state.slots(),
                state.lastRecommendations()
        );
    }

    private static SlotBundle copySlots(SlotBundle source) {
        if (source == null) {
            return SlotBundle.empty();
        }
        return new SlotBundle(
                source.mealTime(),
                source.mood(),
                source.scene(),
                source.healthGoal(),
                source.cuisine(),
                source.taste(),
                source.convenience()
        );
    }
}
