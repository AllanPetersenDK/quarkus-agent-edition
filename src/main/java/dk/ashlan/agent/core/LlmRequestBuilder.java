package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.memory.MemoryService;

import java.util.ArrayList;
import java.util.List;

public class LlmRequestBuilder {
    private final String systemPrompt;
    private final MemoryService memoryService;

    public LlmRequestBuilder(String systemPrompt, MemoryService memoryService) {
        this.systemPrompt = systemPrompt;
        this.memoryService = memoryService;
    }

    public List<LlmMessage> build(ExecutionContext context) {
        List<LlmMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(LlmMessage.system(systemPrompt));
        }
        if (memoryService != null) {
            memoryService.relevantMemories(context.getSessionId(), context.getInput())
                    .forEach(memory -> messages.add(LlmMessage.system("Memory: " + memory)));
        }
        messages.addAll(context.getMessages());
        return messages;
    }
}
