package dk.ashlan.agent.tools;

import dk.ashlan.agent.memory.MemoryService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class RecallMemoryTool extends AbstractTool {
    private final MemoryService memoryService;

    public RecallMemoryTool(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "recall-memory";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Search cross-session memory for relevant experience snippets.");
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        String sessionId = String.valueOf(arguments.getOrDefault("sessionId", "default"));
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        return String.join(" | ", memoryService.relevantMemories(sessionId, query));
    }
}
