package dk.ashlan.agent.core.callback;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.AgentStepResult;
import dk.ashlan.agent.core.AfterLlmContext;
import dk.ashlan.agent.core.AfterRunContext;
import dk.ashlan.agent.core.AfterToolContext;
import dk.ashlan.agent.core.BeforeLlmContext;
import dk.ashlan.agent.core.BeforeToolContext;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentCallbackIntegrationTest {
    @Test
    void invokesHooksInOrderDuringARun() {
        List<String> events = new ArrayList<>();
        AgentCallback recordingCallback = new AgentCallback() {
            @Override
            public void beforeLlm(BeforeLlmContext context) {
                events.add("before-llm:" + context.stepNumber());
            }

            @Override
            public void afterLlm(AfterLlmContext context) {
                events.add("after-llm:" + context.stepNumber());
            }

            @Override
            public boolean beforeTool(BeforeToolContext context) {
                events.add("before-tool:" + context.toolCall().toolName());
                return true;
            }

            @Override
            public dk.ashlan.agent.tools.JsonToolResult afterTool(AfterToolContext context) {
                events.add("after-tool:" + context.toolCall().toolName());
                return context.toolResult();
            }

            @Override
            public void afterRun(AfterRunContext context) {
                events.add("after-run:" + context.result().finalAnswer());
            }
        };

        AtomicInteger calls = new AtomicInteger();
        LlmClient llmClient = (messages, registry, context) -> {
            if (calls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("calculator", Map.of("expression", "25 * 4"), "call-1")));
            }
            return LlmCompletion.answer("The result is 100");
        };

        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool()));
        MemoryService memoryService = new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                llmClient,
                toolRegistry,
                new ToolExecutor(toolRegistry),
                memoryService,
                new SessionManager(),
                List.of(recordingCallback),
                3,
                "system prompt"
        );

        AgentRunResult result = orchestrator.run("What is 25 * 4?", "callback-session");

        assertEquals("The result is 100", result.finalAnswer());
        assertEquals(List.of(
                "before-llm:1",
                "after-llm:1",
                "before-tool:calculator",
                "after-tool:calculator",
                "before-llm:2",
                "after-llm:2",
                "after-run:The result is 100"
        ), events);
    }

    @Test
    void runPersistsCompactMemoryAfterCompletionEvenWithoutAfterRunCallback() {
        SessionManager sessionManager = new SessionManager();
        InMemoryTaskMemoryStore store = new InMemoryTaskMemoryStore();
        MemoryService memoryService = new MemoryService(sessionManager, store, new MemoryExtractionService());
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new CalculatorTool()));
        AtomicInteger calls = new AtomicInteger();
        LlmClient llmClient = (messages, registry, context) -> {
            if (calls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("calculator", Map.of("expression", "25 * 4"), "call-1")));
            }
            return LlmCompletion.answer("The result is 100");
        };

        AgentOrchestrator orchestrator = new AgentOrchestrator(
                llmClient,
                toolRegistry,
                new ToolExecutor(toolRegistry),
                memoryService,
                new SessionManager(),
                List.of(),
                3,
                "system prompt"
        );

        AgentStepResult stepResult = orchestrator.step("What is 25 * 4?", "memory-session");
        assertTrue(stepResult.toolResults().get(0).success());
        assertTrue(memoryService.relevantMemories("memory-session", "100").isEmpty());

        AgentRunResult result = orchestrator.run("What is 25 * 4?", "memory-session");
        assertEquals("The result is 100", result.finalAnswer());
        assertTrue(memoryService.relevantMemories("memory-session", "100").stream()
                .anyMatch(memory -> memory.contains("The result is 100")));
    }
}
