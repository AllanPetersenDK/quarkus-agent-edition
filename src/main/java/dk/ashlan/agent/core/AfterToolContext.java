package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.JsonToolResult;

public record AfterToolContext(
        String sessionId,
        int stepNumber,
        LlmToolCall toolCall,
        JsonToolResult toolResult
) {
}
