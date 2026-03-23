package dk.ashlan.agent.llm;

import java.util.List;

public record LlmRequest(List<LlmMessage> messages) {
    public static LlmRequest of(String userMessage) {
        return new LlmRequest(List.of(LlmMessage.user(userMessage)));
    }
}
