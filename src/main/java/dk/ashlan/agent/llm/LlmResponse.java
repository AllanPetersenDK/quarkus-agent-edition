package dk.ashlan.agent.llm;

import java.util.List;

public record LlmResponse(String content, List<LlmToolCall> toolCalls) {
    public static LlmResponse answer(String content) {
        return new LlmResponse(content, List.of());
    }

    public static LlmResponse toolCalls(List<LlmToolCall> toolCalls) {
        return new LlmResponse(null, toolCalls);
    }
}
