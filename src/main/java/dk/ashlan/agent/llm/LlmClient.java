package dk.ashlan.agent.llm;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.tools.ToolRegistry;

import java.util.List;

public interface LlmClient {
    LlmCompletion complete(List<LlmMessage> messages, ToolRegistry toolRegistry, ExecutionContext context);
}
