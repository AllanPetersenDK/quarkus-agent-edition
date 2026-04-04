package dk.ashlan.agent.tools;

import dk.ashlan.agent.core.ExecutionContext;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.memory.MemoryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ProcessLlmRequestTool {
    private final MemoryService memoryService;

    @Inject
    public ProcessLlmRequestTool(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    public List<LlmMessage> inject(ExecutionContext context) {
        if (memoryService == null || context == null) {
            return List.of();
        }
        List<LlmMessage> injected = new ArrayList<>();
        memoryService.relevantMemories(context.getSessionId(), context.getInput())
                .forEach(memory -> injected.add(LlmMessage.system("Memory: " + memory)));
        return injected;
    }
}
