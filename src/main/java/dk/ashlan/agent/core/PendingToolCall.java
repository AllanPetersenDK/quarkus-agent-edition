package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmToolCall;

import java.util.Map;

public record PendingToolCall(
        String sessionId,
        int stepNumber,
        String input,
        LlmToolCall toolCall,
        String confirmationMessage
) {
    public Map<String, Object> arguments() {
        return toolCall == null ? Map.of() : toolCall.arguments();
    }
}
