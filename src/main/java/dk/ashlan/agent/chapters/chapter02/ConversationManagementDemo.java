package dk.ashlan.agent.chapters.chapter02;

import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.core.LlmRequestBuilder;
import dk.ashlan.agent.memory.MemoryService;

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

    public List<LlmMessage> buildPrompt(String input, MemoryService memoryService) {
        ExecutionContext context = new ExecutionContext(input, "chapter-02");
        messages.forEach(message -> context.addAssistantMessage(message.content()));
        return new LlmRequestBuilder("You are a chapter 02 conversation demo.", memoryService).build(context);
    }
}
