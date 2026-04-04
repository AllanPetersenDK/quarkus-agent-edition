package dk.ashlan.agent.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.cache.CacheManager;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class TaskMemoryRankingCacheTest {
    @Inject
    TaskMemoryRankingCache rankingCache;

    @Inject
    CacheManager cacheManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void resetStatistics() {
        cacheManager.getCache(CachedTaskMemoryRankingScore.CACHE_NAME)
                .ifPresent(cache -> cache.invalidateAll().await().indefinitely());
        rankingCache.resetStatistics();
    }

    @Test
    void sameInputIsCachedAndMatchesThePureRankingHelper() {
        TaskMemory memory = new TaskMemory(
                "session-1",
                "goal",
                "Problem: choose database | Result: PostgreSQL",
                "choose database",
                "preference disclosure",
                "PostgreSQL",
                Boolean.TRUE,
                null
        );

        double first = rankingCache.score(memory, "PostgreSQL");
        double second = rankingCache.score(memory, "PostgreSQL");

        assertEquals(TaskMemoryRanking.score(memory, "PostgreSQL"), first);
        assertEquals(first, second);
        assertEquals(1, rankingCache.computationCount());
    }

    @Test
    void differentInputsDoNotShareTheCachedRankingResult() {
        TaskMemory memory = new TaskMemory(
                "session-1",
                "goal",
                "Problem: choose database | Result: PostgreSQL",
                "choose database",
                "preference disclosure",
                "PostgreSQL",
                Boolean.TRUE,
                null
        );

        double postgreSQLScore = rankingCache.score(memory, "PostgreSQL");
        double quarkusScore = rankingCache.score(memory, "Quarkus");

        assertNotEquals(postgreSQLScore, quarkusScore);
        assertEquals(2, rankingCache.computationCount());
    }

    @Test
    void jdbcStoreUsesTheCachedRankingHelperWithoutChangingRetrievalResults() {
        JdbcTaskMemoryStore store = new JdbcTaskMemoryStore(dataSource(), new ObjectMapper(), rankingCache);
        TaskMemory first = new TaskMemory(
                "session-1",
                "goal",
                "Problem: choose database | Result: PostgreSQL",
                "choose database",
                "preference disclosure",
                "PostgreSQL",
                Boolean.TRUE,
                null
        );
        TaskMemory second = new TaskMemory(
                "session-2",
                "goal",
                "Problem: choose editor | Result: Neovim",
                "choose editor",
                "preference disclosure",
                "Neovim",
                Boolean.TRUE,
                null
        );

        store.save(first);
        store.save(second);

        List<TaskMemory> firstLookup = store.findRelevant("session-3", "PostgreSQL", 5);
        int computationsAfterFirstLookup = rankingCache.computationCount();
        List<TaskMemory> secondLookup = store.findRelevant("session-4", "PostgreSQL", 5);

        assertTrue(computationsAfterFirstLookup > 0);
        assertEquals(computationsAfterFirstLookup, rankingCache.computationCount());
        assertEquals(firstLookup, secondLookup);
        assertTrue(firstLookup.get(0).result().contains("PostgreSQL"));
    }

    private JdbcDataSource dataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:file:" + tempDir.resolve("task-memory-cache").toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");
        return dataSource;
    }
}
