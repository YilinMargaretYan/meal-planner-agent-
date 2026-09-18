package com.diet.agent.workflow;

import com.diet.model.ClarifyResult;
import com.diet.service.clarify.ClarifyAgentService;
import org.springframework.stereotype.Component;

/** Adapter that executes the clarification specialist through the common worker contract. */
@Component
public class ClarifyAgentWorker implements AgentWorker<NoAgentInput, ClarifyResult> {

    private static final String WORKER_NAME = "ClarifyAgent";

    private final ClarifyAgentService clarifyAgentService;

    public ClarifyAgentWorker(ClarifyAgentService clarifyAgentService) {
        this.clarifyAgentService = clarifyAgentService;
    }

    @Override
    public AgentResult<ClarifyResult> execute(AgentRunContext context, NoAgentInput input) {
        if (context.llmCallBudget() < 1) {
            throw new IllegalArgumentException("ClarifyAgent requires one LLM-call budget unit");
        }

        long startedAt = System.nanoTime();
        ClarifyAgentService.Decision decision = clarifyAgentService.decideWithMetadata(
                context.sessionId(),
                context.userInput(),
                context.session().slots()
        );
        ClarifyResult clarify = decision.result();
        return new AgentResult<>(
                WORKER_NAME,
                decision.fallbackUsed() ? AgentExecutionStatus.FALLBACK : AgentExecutionStatus.SUCCESS,
                clarify,
                decision.fallbackUsed() ? 0.2 : 1.0,
                elapsedMs(startedAt),
                decision.fallbackUsed() ? "Clarification template fallback was used." : null
        );
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
