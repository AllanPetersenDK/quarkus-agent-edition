package dk.ashlan.agent.chapters.chapter02;

import dk.ashlan.agent.llm.LlmMessage;

import java.util.ArrayList;
import java.util.List;

public class ConversationManagementDemo {
    private final List<LlmMessage> messages = new ArrayList<>();

    public void add(String userMessage) {
        messages.add(LlmMessage.user(userMessage));
    }

    public List<LlmMessage> messages() {
        return List.copyOf(messages);
    }
}
