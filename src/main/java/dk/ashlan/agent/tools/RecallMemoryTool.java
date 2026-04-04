package dk.ashlan.agent.tools;

import dk.ashlan.agent.memory.TaskMemory;
import dk.ashlan.agent.memory.MemoryService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<TaskMemory> memories = memoryService.longTermMemories(sessionId, query, 3);
        if (memories.isEmpty()) {
            return "No relevant memories found.";
        }
        return memories.stream()
                .map(this::format)
                .collect(Collectors.joining("\n"));
    }

    private String format(TaskMemory memory) {
        StringBuilder builder = new StringBuilder();
        builder.append("Problem: ").append(firstNonBlank(memory.problem(), memory.task()));
        if (memory.approach() != null && !memory.approach().isBlank()) {
            builder.append(" | Approach: ").append(memory.approach());
        }
        builder.append(" | Result: ").append(firstNonBlank(memory.result(), memory.memory()));
        if (memory.errorAnalysis() != null && !memory.errorAnalysis().isBlank()) {
            builder.append(" | Error analysis: ").append(memory.errorAnalysis());
        }
        return builder.toString();
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : (fallback == null ? "" : fallback);
    }
}
