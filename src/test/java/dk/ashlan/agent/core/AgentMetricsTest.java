package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentMetricsTest {
    @Test
    void recordsRunMetricsForSuccessfulAgentRun() {
        LlmClient llmClient = (messages, toolRegistry, context) -> LlmCompletion.answer("hello");
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool()));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        MemoryService memoryService = new MemoryService(
                new SessionManager(),
                new InMemoryTaskMemoryStore(),
                new MemoryExtractionService()
        );
        AgentOrchestrator orchestrator = new AgentOrchestrator(llmClient, toolRegistry, toolExecutor, memoryService, 3, "system prompt");
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        orchestrator.meterRegistry = meterRegistry;

        AgentRunResult result = orchestrator.run("say hello", "session-1");

        assertEquals("hello", result.finalAnswer());
        assertEquals(1.0, meterRegistry.counter("agent.run.total", "stopReason", "FINAL_ANSWER").count());
        assertEquals(1L, meterRegistry.find("agent.run.duration").timer().count());
    }
}
