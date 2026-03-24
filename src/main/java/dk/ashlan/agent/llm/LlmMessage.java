package dk.ashlan.agent.llm;

public record LlmMessage(String role, String content, String name, String toolCallId) {
    public static LlmMessage system(String content) {
        return new LlmMessage("system", content, null, null);
    }

    public static LlmMessage user(String content) {
        return new LlmMessage("user", content, null, null);
    }

    public static LlmMessage assistant(String content) {
        return new LlmMessage("assistant", content, null, null);
    }

    public static LlmMessage tool(String name, String content) {
        return new LlmMessage("tool", content, name, null);
    }

    public static LlmMessage tool(String name, String toolCallId, String content) {
        return new LlmMessage("tool", content, name, toolCallId);
    }
}
