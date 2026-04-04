package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;

import java.util.List;

public record BeforeLlmContext(
        String sessionId,
        int stepNumber,
        List<LlmMessage> messages
) {
}
