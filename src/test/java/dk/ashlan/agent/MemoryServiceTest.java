package dk.ashlan.agent;

import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryWriteDecision;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.TaskMemory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MemoryServiceTest {
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

        assertTrue(memoryService.relevantMemories("session-3", "Copenhagen").get(0).contains("Copenhagen"));
        assertTrue(memoryService.longTermMemories("session-3", "Copenhagen", 1).get(0).problem().contains("location"));
    }

    @Test
    void retrievalRanksTheMostRelevantMemoryFirst() {
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());

        memoryService.remember("session-1", "goal", "Remember that my favorite database is PostgreSQL.");
        memoryService.remember("session-2", "goal", "Remember that PostgreSQL is important for my current project.");

        List<TaskMemory> memories = memoryService.longTermMemories("session-3", "favorite database PostgreSQL", 2);

        assertTrue(memories.get(0).memory().contains("favorite database"));
    }

    @Test
    void trivialNoiseIsSkippedAndDuplicateFactsAreNotStoredTwiceAcrossSessions() {
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());

        assertEquals(MemoryWriteDecision.SKIP, memoryService.remember("session-1", "goal", "hello"));
        assertEquals(MemoryWriteDecision.ADD, memoryService.remember("session-1", "goal", "Remember that my favorite database is PostgreSQL."));
        assertEquals(MemoryWriteDecision.SKIP, memoryService.remember("session-2", "goal", "Remember that my favorite database is PostgreSQL!"));
        assertTrue(memoryService.relevantMemories("session-2", "PostgreSQL").size() == 1);
    }

    @Test
    void danishProfileStatementsCreateCompactMemorySignal() {
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());

        assertEquals(MemoryWriteDecision.ADD, memoryService.remember(
                "session-2",
                "profile",
                "Mit navn er Alice, og jeg arbejder som marketer."
        ));

        assertTrue(memoryService.relevantMemories("session-2", "Alice").stream()
                .anyMatch(memory -> memory.contains("User name: Alice")));
        assertTrue(memoryService.longTermMemories("session-2", "Alice", 1).get(0).taskSummary().contains("danish-profile"));
        assertTrue(memoryService.longTermMemories("session-2", "Alice", 1).get(0).finalAnswer().contains("Alice"));
    }
}
