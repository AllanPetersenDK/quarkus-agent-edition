package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmToolCall;

public record BeforeToolContext(
        String sessionId,
        int stepNumber,
        LlmToolCall toolCall
) {
}
