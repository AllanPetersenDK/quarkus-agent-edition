package dk.ashlan.agent.memory;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryTaskMemoryStore implements TaskMemoryStore {
    private final List<TaskMemory> memories = new CopyOnWriteArrayList<>();

    @Override
    public void save(TaskMemory taskMemory) {
        memories.add(taskMemory);
    }

    @Override
    public List<TaskMemory> findRelevant(String sessionId, String query, int limit) {
        if (TaskMemoryRanking.normalize(query).isBlank() || limit <= 0) {
            return List.of();
        }
        return memories.stream()
                .sorted(Comparator.comparingDouble((TaskMemory memory) -> TaskMemoryRanking.score(memory, query)).reversed())
                .limit(limit)
                .toList();
    }
}
