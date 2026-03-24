package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentOrchestratorTest {
    @Test
    void forwardsToolCallIdsBackIntoTheNextModelRequest() {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<List<LlmMessage>> secondRequest = new AtomicReference<>();
        LlmClient llmClient = (messages, toolRegistry, context) -> {
            if (calls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("calculator", Map.of("expression", "25 * 4"), "call-123")));
            }
            secondRequest.set(List.copyOf(messages));
            return LlmCompletion.answer("25 * 4 = 100");
        };

        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool()));
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        MemoryService memoryService = new MemoryService(
                new SessionManager(),
                new InMemoryTaskMemoryStore(),
                new MemoryExtractionService()
        );
        AgentOrchestrator orchestrator = new AgentOrchestrator(llmClient, toolRegistry, toolExecutor, memoryService, 3, "system prompt");

        AgentRunResult result = orchestrator.run("What is 25 * 4?", "session-1");

        assertEquals("25 * 4 = 100", result.finalAnswer());
        assertTrue(secondRequest.get().stream().anyMatch(message ->
                "tool".equals(message.role())
                        && "calculator".equals(message.name())
                        && "call-123".equals(message.toolCallId())
        ));
    }
}
