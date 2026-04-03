package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentOrchestratorStepTest {
    @Test
    void stepExecutesExactlyOneToolCallingCycle() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient llmClient = (messages, toolRegistry, context) -> {
            if (calls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("calculator", Map.of("expression", "25 * 4"), "call-1")));
            }
            return LlmCompletion.answer("25 * 4 = 100");
        };

        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool()));
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                llmClient,
                toolRegistry,
                new ToolExecutor(toolRegistry),
                new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService()),
                3,
                "system prompt"
        );

        AgentStepResult result = orchestrator.step("What is 25 * 4?", "step-session");

        assertEquals(1, calls.get());
        assertEquals(1, result.stepNumber());
        assertFalse(result.isFinal());
        assertTrue(result.toolCalls().stream().anyMatch(call -> "calculator".equals(call.toolName())));
        assertTrue(result.toolResults().get(0).output().contains("100"));
        assertNotNull(result.traceEntries());
    }

    @Test
    void stepReturnsFinalAnswerWhenTheModelAnswersDirectly() {
        AtomicInteger calls = new AtomicInteger();
        LlmClient llmClient = (messages, toolRegistry, context) -> {
            calls.incrementAndGet();
            return LlmCompletion.answer("answer: direct chapter-4 result");
        };

        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool()));
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                llmClient,
                toolRegistry,
                new ToolExecutor(toolRegistry),
                new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService()),
                3,
                "system prompt"
        );

        AgentStepResult result = orchestrator.step("Say hello", "final-session");

        assertEquals(1, calls.get());
        assertEquals("answer: direct chapter-4 result", result.finalAnswer());
        assertTrue(result.isFinal());
    }
}
