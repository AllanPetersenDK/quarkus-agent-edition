package dk.ashlan.agent.tools;

import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.MemoryService;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.stream.Collectors;

@ApplicationScoped
public class ConversationSearchTool extends AbstractTool {
    private final MemoryService memoryService;
    private final SessionManager sessionManager;

    public ConversationSearchTool(MemoryService memoryService, SessionManager sessionManager) {
        this.memoryService = memoryService;
        this.sessionManager = sessionManager;
    }

    @Override
    public String name() {
        return "conversation-search";
    }

    @Override
    public ToolDefinition definition() {
        return new ToolDefinition(name(), "Search session conversation snippets, then fall back to long-term memory if needed.");
    }

    @Override
    protected String executeSafely(Map<String, Object> arguments) {
        String sessionId = String.valueOf(arguments.getOrDefault("sessionId", "default"));
        String query = String.valueOf(arguments.getOrDefault("query", ""));
        List<String> matches = sessionMessages(sessionId, query);
        if (!matches.isEmpty()) {
            return String.join(" | ", matches);
        }
        return String.join(" | ", memoryService.relevantMemories(sessionId, query));
    }

    private List<String> sessionMessages(String sessionId, String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalizedQuery = query.toLowerCase(Locale.ROOT).trim();
        return sessionManager.session(sessionId).messages().stream()
                .map(this::format)
                .filter(entry -> entry.toLowerCase(Locale.ROOT).contains(normalizedQuery))
                .collect(Collectors.toList());
    }

    private String format(LlmMessage message) {
        String role = message.role() == null ? "unknown" : message.role();
        String content = message.content() == null ? "" : message.content().trim().replaceAll("\\s+", " ");
        return role + ": " + content;
    }
}
