package dk.ashlan.agent.llm;

import java.util.List;

public record LlmRequest(List<LlmMessage> messages) {
    public static LlmRequest of(String userMessage) {
        return new LlmRequest(List.of(LlmMessage.user(userMessage)));
    }

    public int estimatedTokenCount() {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        int charCount = 0;
        for (LlmMessage message : messages) {
            charCount += 4;
            if (message.role() != null) {
                charCount += message.role().length();
            }
            if (message.name() != null) {
                charCount += message.name().length();
            }
            if (message.content() != null) {
                charCount += message.content().length();
            }
            if (message.toolCallId() != null) {
                charCount += message.toolCallId().length();
            }
            if (message.toolCalls() != null) {
                charCount += message.toolCalls().toString().length();
            }
        }
        return Math.max(1, (charCount + 3) / 4);
    }
}
