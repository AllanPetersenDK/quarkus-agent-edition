package dk.ashlan.agent.llm;

import java.util.List;

public record LlmMessage(String role, String content, String name, String toolCallId, List<LlmToolCall> toolCalls) {
    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null, List.of());
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null, List.of());
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content, null, null, List.of());
    }

    public static LlmMessage assistant(List<LlmToolCall> toolCalls) {
        return new LlmMessage("assistant", null, null, null, toolCalls == null ? List.of() : List.copyOf(toolCalls));
    }

    public static LlmMessage tool(String name, String content) {
        return new LlmMessage("tool", content, name, null, List.of());
    }

    public static LlmMessage tool(String name, String toolCallId, String content) {
        return new LlmMessage("tool", content, name, toolCallId, List.of());
    }
}
