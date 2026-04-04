package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AfterRunContext;
import dk.ashlan.agent.core.AfterLlmContext;
import dk.ashlan.agent.core.AfterToolContext;
import dk.ashlan.agent.core.BeforeLlmContext;
import dk.ashlan.agent.core.BeforeToolContext;
import dk.ashlan.agent.tools.JsonToolResult;

public interface AgentCallback {
    default void beforeLlm(BeforeLlmContext context) {
    }

    default void afterLlm(AfterLlmContext context) {
    }

    default boolean beforeTool(BeforeToolContext context) {
        return true;
    }

    default JsonToolResult afterTool(AfterToolContext context) {
        return context.toolResult();
    }

    default void afterRun(AfterRunContext context) {
    }
}
