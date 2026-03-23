package dk.ashlan.agent.tools;

import dk.ashlan.agent.memory.MemoryService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;

@ApplicationScoped
public class CoreMemoryUpsertTool implements Tool {
    private final MemoryService memoryService;

    public CoreMemoryUpsertTool(MemoryService memoryService) {
        this.memoryService = memoryService;
    }

    @Override
    public String name() {
        return "core-memory-upsert";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Store a memory snippet for a session.");
    }

    @Override
    public JsonToolResult execute(Map<String, Object> arguments) {
        String sessionId = String.valueOf(arguments.getOrDefault("sessionId", "default"));
        String task = String.valueOf(arguments.getOrDefault("task", "memory"));
        String value = String.valueOf(arguments.getOrDefault("value", ""));
        memoryService.remember(sessionId, task, value);
        return JsonToolResult.success(name(), "Stored memory for session " + sessionId);
    }
}
