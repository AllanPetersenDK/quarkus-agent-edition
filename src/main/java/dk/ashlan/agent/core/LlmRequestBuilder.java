package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.tools.ProcessLlmRequestTool;

import java.util.ArrayList;
import java.util.List;

public class LlmRequestBuilder {
    private final String systemPrompt;
    private final MemoryService memoryService;
    private final ProcessLlmRequestTool processLlmRequestTool;

    public LlmRequestBuilder(String systemPrompt, MemoryService memoryService) {
        this(systemPrompt, memoryService, null);
    }

    public LlmRequestBuilder(String systemPrompt, MemoryService memoryService, ProcessLlmRequestTool processLlmRequestTool) {
        this.systemPrompt = systemPrompt;
        this.memoryService = memoryService;
        this.processLlmRequestTool = processLlmRequestTool;
    }

    public List<LlmMessage> build(ExecutionContext context) {
        List<LlmMessage> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(LlmMessage.system(systemPrompt));
        }
        if (processLlmRequestTool != null) {
            messages.addAll(processLlmRequestTool.inject(context));
        } else if (memoryService != null) {
            memoryService.relevantMemories(context.getSessionId(), context.getInput())
                    .forEach(memory -> messages.add(LlmMessage.system("Memory: " + memory)));
        }
        messages.addAll(context.getMessages());
        return messages;
    }
}
