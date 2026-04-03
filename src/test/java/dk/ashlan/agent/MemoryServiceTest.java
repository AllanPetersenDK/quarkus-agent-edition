package dk.ashlan.agent;

import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void longTermRetrievalReturnsRelevantMemory() {
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());

        memoryService.remember("session-1", "goal", "I live in Copenhagen and like Quarkus");
        memoryService.remember("session-1", "goal", "I also work with Java 21");

        assertTrue(memoryService.relevantMemories("session-1", "Copenhagen").get(0).contains("Copenhagen"));
    }
}
