package com.diet.agent.workflow;

/** Common contract for an independently testable specialist worker. */
public interface AgentWorker<I, O> {
    AgentResult<O> execute(AgentRunContext context, I input);
}
