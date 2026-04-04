package dk.ashlan.agent.tools;

import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.TaskMemory;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecallMemoryToolTest {
    @Test
    void returnsRelevantCrossSessionMemoryWithStructuredFormatting() {
        InMemoryTaskMemoryStore store = new InMemoryTaskMemoryStore();
        store.save(new TaskMemory(
                "session-1",
                "goal",
                "Problem: choose database",
                "choose database",
                "preference disclosure",
                "PostgreSQL",
                Boolean.TRUE,
                "No issues detected"
        ));
        MemoryService memoryService = new MemoryService(store, new MemoryExtractionService());
        RecallMemoryTool tool = new RecallMemoryTool(memoryService);

        String output = tool.execute(Map.of("sessionId", "session-2", "query", "PostgreSQL")).output();

        assertTrue(output.contains("Problem:"));
        assertTrue(output.contains("Summary:"));
        assertTrue(output.contains("Approach:"));
        assertTrue(output.contains("Result:"));
        assertTrue(output.contains("PostgreSQL"));
        assertTrue(output.contains("Error analysis:"));
    }

    @Test
    void omitsEmptyFieldsWhenTheyAreNotPresent() {
        InMemoryTaskMemoryStore store = new InMemoryTaskMemoryStore();
        store.save(new TaskMemory("session-1", "goal", "Remember that my favorite editor is Neovim."));
        MemoryService memoryService = new MemoryService(store, new MemoryExtractionService());
        RecallMemoryTool tool = new RecallMemoryTool(memoryService);

        String output = tool.execute(Map.of("sessionId", "session-1", "query", "Neovim")).output();

        assertTrue(output.contains("Problem:"));
        assertTrue(output.contains("Result:"));
        assertFalse(output.contains("Summary:"));
        assertFalse(output.contains("Approach:"));
        assertFalse(output.contains("Error analysis:"));
    }
}
