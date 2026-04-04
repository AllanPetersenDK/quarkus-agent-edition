package dk.ashlan.agent.memory;

import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
class CachedTaskMemoryRankingScore {
    static final String CACHE_NAME = "task-memory-ranking";

    private final AtomicInteger computationCount = new AtomicInteger();

    @CacheResult(cacheName = CACHE_NAME)
    public double score(TaskMemoryRankingKey key) {
        computationCount.incrementAndGet();
        return TaskMemoryRanking.score(key.memory(), key.query());
    }

    int computationCount() {
        return computationCount.get();
    }

    void resetStatistics() {
        computationCount.set(0);
    }
}
