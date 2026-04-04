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
                .collect(Collectors.joining("\n\n"));
    }

    private String format(TaskMemory memory) {
        StringBuilder builder = new StringBuilder();
        appendField(builder, "Problem", firstNonBlank(memory.problem(), memory.task()));
        appendField(builder, "Summary", memory.taskSummary());
        appendField(builder, "Approach", memory.approach());
        appendField(builder, "Result", firstNonBlank(memory.result(), memory.memory()));
        appendField(builder, "Error analysis", memory.errorAnalysis());
        return builder.toString();
    }

    private void appendField(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(label).append(": ").append(value.trim());
    }

    private String firstNonBlank(String first, String fallback) {
        return first != null && !first.isBlank() ? first : (fallback == null ? "" : fallback);
    }
}
