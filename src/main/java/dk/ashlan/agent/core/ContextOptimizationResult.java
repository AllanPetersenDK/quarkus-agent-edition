package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;

import java.util.List;

public record ContextOptimizationResult(
        List<LlmMessage> messages,
        String strategy,
        int originalTokenCount,
        int projectedTokenCount
) {
    public boolean changed() {
        return messages != null && !"none".equals(strategy);
    }

    public static ContextOptimizationResult unchanged(List<LlmMessage> messages, int tokenCount) {
        return new ContextOptimizationResult(messages == null ? List.of() : List.copyOf(messages), "none", tokenCount, tokenCount);
    }
}
