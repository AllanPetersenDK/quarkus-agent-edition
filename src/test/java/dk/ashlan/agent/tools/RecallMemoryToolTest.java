package dk.ashlan.agent.tools;

import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class RecallMemoryToolTest {
    @Test
    void returnsRelevantCrossSessionMemory() {
        MemoryService memoryService = new MemoryService(new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        memoryService.remember("session-1", "goal", "Remember that my favorite database is PostgreSQL.");
        RecallMemoryTool tool = new RecallMemoryTool(memoryService);

        String output = tool.execute(Map.of("sessionId", "session-1", "query", "favorite database")).output();

        assertTrue(output.contains("PostgreSQL"));
    }
}
