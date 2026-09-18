package com.diet.agent.workflow;

import com.diet.service.recommend.RecommendResponseAgentService;
import org.springframework.stereotype.Component;

/** Adapter that turns ranked candidates into user-facing recommendations and copy. */
@Component
public class RecommendResponseAgentWorker implements AgentWorker<RecommendResponseInput, RecommendResponseAgentService.Result> {

    private static final String WORKER_NAME = "RecommendResponseAgent";

    private final RecommendResponseAgentService recommendResponseAgentService;

    public RecommendResponseAgentWorker(RecommendResponseAgentService recommendResponseAgentService) {
        this.recommendResponseAgentService = recommendResponseAgentService;
    }

    @Override
    public AgentResult<RecommendResponseAgentService.Result> execute(
            AgentRunContext context,
            RecommendResponseInput input
    ) {
        if (context.llmCallBudget() < 1) {
            throw new IllegalArgumentException("RecommendResponseAgent requires one LLM-call budget unit");
        }

        long startedAt = System.nanoTime();
        RecommendResponseAgentService.Result result = recommendResponseAgentService.recommendAndRespond(
                context.sessionId(),
                context.userInput(),
                context.session().sourceMode(),
                context.session().slots(),
                input.rankedMeals()
        );
        return new AgentResult<>(
                WORKER_NAME,
                result.fallbackUsed() ? AgentExecutionStatus.FALLBACK : AgentExecutionStatus.SUCCESS,
                result,
                result.fallbackUsed() ? 0.2 : 1.0,
                elapsedMs(startedAt),
                result.fallbackUsed() ? "Recommendation template fallback was used." : null
        );
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
