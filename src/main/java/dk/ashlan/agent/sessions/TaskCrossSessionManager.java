package dk.ashlan.agent.sessions;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class TaskCrossSessionManager extends BaseCrossSessionManager {
    private final List<String> memories = new CopyOnWriteArrayList<>();

    @Override
    public void remember(String key, String value) {
        memories.add(key + ":" + value);
    }

    @Override
    public List<String> search(String query, int limit) {
        return memories.stream()
                .filter(memory -> memory.contains(query))
                .limit(limit)
                .toList();
    }
}
