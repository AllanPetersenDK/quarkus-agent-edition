package dk.ashlan.agent.llm;

import java.util.List;

public record LlmCompletion(String content, List<LlmToolCall> toolCalls) {
    public static LlmCompletion answer(String content) {
        return new LlmCompletion(content, List.of());
    }

    public static LlmCompletion toolCalls(List<LlmToolCall> toolCalls) {
        return new LlmCompletion(null, toolCalls);
    }
}
