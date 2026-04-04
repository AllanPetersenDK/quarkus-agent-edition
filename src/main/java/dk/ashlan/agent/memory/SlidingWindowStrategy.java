package dk.ashlan.agent.memory;

import dk.ashlan.agent.llm.LlmMessage;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SlidingWindowStrategy implements MemoryStrategy {
    private final int windowSize = 5;

    @Override
    public String name() {
        return "sliding-window";
    }

    @Override
    public List<String> update(List<String> memory, String event) {
        ArrayList<String> updated = new ArrayList<>(memory);
        updated.add(event);
        if (updated.size() > windowSize) {
            return updated.subList(updated.size() - windowSize, updated.size());
        }
        return updated;
    }

    @Override
    public List<String> retrieve(List<String> memory, String query) {
        return memory.stream().filter(entry -> entry.contains(query)).toList();
    }

    public List<LlmMessage> trimConversation(List<LlmMessage> messages) {
        return trimConversation(messages, windowSize);
    }

    public List<LlmMessage> trimConversation(List<LlmMessage> messages, int maxMessages) {
        if (messages == null || messages.isEmpty() || maxMessages <= 0) {
            return List.of();
        }
        List<LlmMessage> copied = List.copyOf(messages);
        if (copied.size() <= maxMessages) {
            return copied;
        }
        int start = Math.max(0, copied.size() - maxMessages);
        List<LlmMessage> trimmed = new ArrayList<>(copied.subList(start, copied.size()));
        if (!copied.isEmpty() && "system".equals(copied.get(0).role()) && trimmed.stream().noneMatch(message -> "system".equals(message.role()))) {
            trimmed.add(0, copied.get(0));
            while (trimmed.size() > maxMessages) {
                trimmed.remove(1);
            }
        }
        return List.copyOf(trimmed);
    }
}
