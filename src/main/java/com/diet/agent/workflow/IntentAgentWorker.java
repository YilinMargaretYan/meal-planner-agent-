package com.diet.agent.workflow;

import com.diet.model.IntentResult;
import com.diet.service.intent.IntentAgentService;
import org.springframework.stereotype.Component;

/** Adapter that makes the existing intent specialist conform to the worker contract. */
@Component
public class IntentAgentWorker implements AgentWorker<NoAgentInput, IntentResult> {

    private static final String WORKER_NAME = "IntentAgent";
    private static final double FALLBACK_CONFIDENCE = 0.2;

    private final IntentAgentService intentAgentService;

    public IntentAgentWorker(IntentAgentService intentAgentService) {
        this.intentAgentService = intentAgentService;
    }

    @Override
    public AgentResult<IntentResult> execute(AgentRunContext context, NoAgentInput input) {
        if (context.llmCallBudget() < 1) {
            throw new IllegalArgumentException("IntentAgent requires one LLM-call budget unit");
        }

        long startedAt = System.nanoTime();
        IntentResult intent = intentAgentService.recognize(
                context.sessionId(),
                context.userId(),
                context.userInput(),
                context.session().slots(),
                context.recentHistory()
        );
        boolean fallback = intent.confidence() <= FALLBACK_CONFIDENCE;
        return new AgentResult<>(
                WORKER_NAME,
                fallback ? AgentExecutionStatus.FALLBACK : AgentExecutionStatus.SUCCESS,
                intent,
                intent.confidence(),
                elapsedMs(startedAt),
                fallback ? "Intent service returned a low-confidence fallback." : null
        );
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
