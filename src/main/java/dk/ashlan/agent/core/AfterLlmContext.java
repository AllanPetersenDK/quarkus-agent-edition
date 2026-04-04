package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;

import java.util.List;

public record AfterLlmContext(
        String sessionId,
        int stepNumber,
        List<LlmMessage> messages,
        LlmCompletion completion
) {
}
