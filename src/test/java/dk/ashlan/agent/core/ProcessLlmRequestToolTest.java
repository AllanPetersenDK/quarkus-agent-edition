package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ProcessLlmRequestTool;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessLlmRequestToolTest {
    @Test
    void injectsMemoryWithoutBeingPartOfTheLLMToolRegistry() {
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        memoryService.remember("session-1", "goal", "Remember that my favorite database is PostgreSQL.");

        ProcessLlmRequestTool processLlmRequestTool = new ProcessLlmRequestTool(memoryService);
        LlmRequestBuilder builder = new LlmRequestBuilder("You are helpful.", memoryService, processLlmRequestTool);
        List<LlmMessage> messages = builder.build(new ExecutionContext("Tell me about PostgreSQL", "session-1"));
        ToolRegistry registry = new ToolRegistry(List.of(new CalculatorTool()));

        assertEquals("system", messages.get(0).role());
        assertEquals("system", messages.get(1).role());
        assertTrue(messages.get(1).content().contains("Memory:"));
        assertTrue(messages.get(1).content().contains("PostgreSQL"));
        assertFalse(registry.definitions().containsKey("process_llm_request"));
    }
}
