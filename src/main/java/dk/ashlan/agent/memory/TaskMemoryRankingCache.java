package dk.ashlan.agent.memory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TaskMemoryRankingCache {
    private final CachedTaskMemoryRankingScore cachedTaskMemoryRankingScore;

    @Inject
    public TaskMemoryRankingCache(CachedTaskMemoryRankingScore cachedTaskMemoryRankingScore) {
        this.cachedTaskMemoryRankingScore = cachedTaskMemoryRankingScore;
    }

    public double score(TaskMemory memory, String query) {
        return cachedTaskMemoryRankingScore.score(new TaskMemoryRankingKey(memory, query));
    }

    int computationCount() {
        return cachedTaskMemoryRankingScore.computationCount();
    }

    void resetStatistics() {
        cachedTaskMemoryRankingScore.resetStatistics();
    }
}
