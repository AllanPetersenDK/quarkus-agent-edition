package dk.ashlan.agent;

import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryWriteDecision;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.JdbcTaskMemoryStore;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.TaskMemory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void rememberDoesNotMutateSessionState() {
        SessionManager sessionManager = new SessionManager();
        InMemoryTaskMemoryStore store = new InMemoryTaskMemoryStore();
        MemoryService memoryService = new MemoryService(sessionManager, store, new MemoryExtractionService());

        memoryService.remember("session-1", "intro", "My name is Ada");

        assertTrue(sessionManager.session("session-1").messages().isEmpty());
    }

    @Test
    void longTermRetrievalReturnsRelevantMemoryAcrossSessions() {
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());

        memoryService.remember("session-1", "goal", "I live in Copenhagen and like Quarkus");
        memoryService.remember("session-2", "goal", "I also work with Java 21");

        TaskMemory memory = memoryService.longTermMemories("session-3", "Copenhagen", 1).get(0);
        assertEquals("session-1", memory.sessionId());
        assertTrue(memory.problem().contains("location"));
        assertTrue(memory.result().contains("Copenhagen"));
    }

    @Test
    void retrievalRanksStructuredFieldsAheadOfRawStringMatches() {
        InMemoryTaskMemoryStore store = new InMemoryTaskMemoryStore();
        store.save(new TaskMemory(
                "session-1",
                "goal",
                "Completely unrelated raw text",
                "database choice",
                "preference disclosure",
                "PostgreSQL",
                null,
                null
        ));
        store.save(new TaskMemory(
                "session-2",
                "goal",
                "database PostgreSQL appears in the raw memory only",
                "general note",
                "miscellaneous",
                "MySQL",
                null,
                null
        ));

        MemoryService memoryService = new MemoryService(new SessionManager(), store, new MemoryExtractionService());
        List<TaskMemory> memories = memoryService.longTermMemories("session-3", "database PostgreSQL", 2);

        assertEquals("session-1", memories.get(0).sessionId());
        assertTrue(memories.get(0).summary().contains("database choice"));
        assertTrue(memories.get(0).result().contains("PostgreSQL"));
    }

    @Test
    void trivialNoiseIsSkippedAndDuplicateFactsAreNotStoredTwiceAcrossSessions() {
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());

        assertEquals(MemoryWriteDecision.SKIP, memoryService.remember("session-1", "goal", "hello"));
        assertEquals(MemoryWriteDecision.ADD, memoryService.remember("session-1", "goal", "Remember that my favorite database is PostgreSQL."));
        assertEquals(MemoryWriteDecision.SKIP, memoryService.remember("session-2", "goal", "Please remember that my favorite database is PostgreSQL."));
        assertEquals(MemoryWriteDecision.SKIP, memoryService.remember("session-3", "goal", "Remember that my favorite database is PostgreSQL."));
        assertEquals(MemoryWriteDecision.ADD, memoryService.remember("session-4", "goal", "Remember that my favorite editor is Neovim."));
        assertTrue(memoryService.longTermMemories("session-2", "PostgreSQL", 2).stream()
                .anyMatch(memory -> memory.result().contains("PostgreSQL")));
        assertTrue(memoryService.longTermMemories("session-4", "Neovim", 1).get(0).result().contains("Neovim"));
    }

    @Test
    void danishProfileStatementsCreateCompactMemorySignal() {
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());

        assertEquals(MemoryWriteDecision.ADD, memoryService.remember(
                "session-2",
                "profile",
                "Mit navn er Alice, og jeg arbejder som marketer."
        ));

        TaskMemory stored = memoryService.longTermMemories("session-2", "Alice", 1).get(0);
        assertTrue(stored.memory().contains("User name: Alice"));
        assertTrue(stored.summary().contains("danish-profile"));
        assertTrue(stored.result().contains("Alice"));
    }

    @Test
    void longAfterRunSignalsAreDeduplicatedWithoutCrashingJdbcPersistence() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:file:" + tempDir.resolve("memory-service-long-signal").toAbsolutePath() + ";AUTO_SERVER=FALSE;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        MemoryService memoryService = new MemoryService(new SessionManager(), new JdbcTaskMemoryStore(dataSource), new MemoryExtractionService());
        String baseSignal = "Remember that my favorite database is PostgreSQL and I prefer concise answers. " + "trace ".repeat(120);
        String nearDuplicateSignal = "Please remember that my favorite database is PostgreSQL and I prefer concise answers. " + "trace ".repeat(110);

        assertEquals(MemoryWriteDecision.ADD, memoryService.remember("session-1", "after-run", baseSignal));
        assertEquals(MemoryWriteDecision.SKIP, memoryService.remember("session-2", "after-run", nearDuplicateSignal));
        assertEquals(1, memoryService.longTermMemories("session-3", "PostgreSQL", 5).size());
        assertTrue(memoryService.longTermMemories("session-3", "PostgreSQL", 1).get(0).result().contains("PostgreSQL"));
    }
}
