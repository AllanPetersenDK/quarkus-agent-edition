package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.tools.ProcessLlmRequestTool;

import java.util.ArrayList;
import java.util.List;

public class LlmRequestBuilder {
    public static final String REQUEST_PREP_TRACE_ATTRIBUTE = "requestPrepTraceEntry";
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
            List<LlmMessage> injectedMessages = processLlmRequestTool.inject(context);
            messages.addAll(injectedMessages);
            if (!injectedMessages.isEmpty()) {
                context.putAttribute(REQUEST_PREP_TRACE_ATTRIBUTE,
                        new AgentTraceEntry("request-prep", "memory-injection:" + injectedMessages.size() + " via hidden-request-prep"));
            }
        } else if (memoryService != null) {
            List<String> relevantMemories = memoryService.relevantMemories(context.getSessionId(), context.getInput());
            relevantMemories.forEach(memory -> messages.add(LlmMessage.system("Memory: " + memory)));
            if (!relevantMemories.isEmpty()) {
                context.putAttribute(REQUEST_PREP_TRACE_ATTRIBUTE,
                        new AgentTraceEntry("request-prep", "memory-injection:" + relevantMemories.size() + " via hidden-request-prep"));
            }
        }
        messages.addAll(context.getMessages());
        return messages;
    }
}
