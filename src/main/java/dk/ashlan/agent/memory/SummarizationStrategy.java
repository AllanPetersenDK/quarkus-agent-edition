package dk.ashlan.agent.memory;

import dk.ashlan.agent.llm.LlmMessage;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@ApplicationScoped
public class SummarizationStrategy implements MemoryStrategy {
    @Override
    public String name() {
        return "summarization";
    }

    @Override
    public List<String> update(List<String> memory, String event) {
        if (memory.isEmpty()) {
            return List.of(event);
        }
        return List.of(memory.get(0) + " | " + event);
    }

    @Override
    public List<String> retrieve(List<String> memory, String query) {
        return memory.stream().filter(entry -> entry.contains(query)).toList();
    }

    public String summarizeConversation(List<LlmMessage> messages) {
        return summarizeConversation(messages, 5);
    }

    public String summarizeConversation(List<LlmMessage> messages, int maxMessages) {
        if (messages == null || messages.isEmpty() || maxMessages <= 0) {
            return "";
        }
        List<LlmMessage> copied = List.copyOf(messages);
        List<LlmMessage> tail = copied.size() <= maxMessages
                ? copied
                : copied.subList(Math.max(0, copied.size() - maxMessages), copied.size());
        String summary = tail.stream()
                .map(this::formatMessage)
                .filter(entry -> !entry.isBlank())
                .collect(Collectors.joining(" | "));
        if (summary.length() > 420) {
            summary = summary.substring(0, 420).trim() + " …";
        }
        return summary;
    }

    private String formatMessage(LlmMessage message) {
        String role = message.role() == null ? "unknown" : message.role().toLowerCase(Locale.ROOT);
        String content = message.content() == null ? "" : message.content().trim().replaceAll("\\s+", " ");
        if (content.length() > 120) {
            content = content.substring(0, 120).trim() + " …";
        }
        if (content.isBlank() && !message.toolCalls().isEmpty()) {
            content = "tool calls: " + message.toolCalls().size();
        }
        return role + ": " + content;
    }
}
