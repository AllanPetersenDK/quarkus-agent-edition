package dk.ashlan.agent.memory;

import java.util.List;

public interface TaskMemoryStore {
    void save(TaskMemory taskMemory);

    List<TaskMemory> findRelevant(String sessionId, String query, int limit);
}
