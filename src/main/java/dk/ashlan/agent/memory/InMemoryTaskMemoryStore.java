package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class InMemoryTaskMemoryStore implements TaskMemoryStore {
    private final List<TaskMemory> memories = new CopyOnWriteArrayList<>();

    @Override
    public void save(TaskMemory taskMemory) {
        memories.add(taskMemory);
    }

    @Override
    public List<TaskMemory> findRelevant(String sessionId, String query, int limit) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        return memories.stream()
                .filter(memory -> memory.sessionId().equals(sessionId))
                .sorted(Comparator.comparingInt((TaskMemory memory) -> score(memory.memory(), normalizedQuery)).reversed())
                .limit(limit)
                .toList();
    }

    private int score(String text, String query) {
        String normalizedText = text.toLowerCase(Locale.ROOT);
        int score = 0;
        for (String token : query.split("\\s+")) {
            if (!token.isBlank() && normalizedText.contains(token)) {
                score++;
            }
        }
        return score;
    }
}
