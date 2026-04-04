package dk.ashlan.agent.core;

import dk.ashlan.agent.llm.LlmClient;
import dk.ashlan.agent.llm.LlmCompletion;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.tools.AbstractTool;
import dk.ashlan.agent.tools.ToolDefinition;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanInTheLoopTest {
    @Test
    void pendingToolCallsPauseTheRunUntilApproved() {
        AtomicInteger toolExecutions = new AtomicInteger();
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new ConfirmableTool(toolExecutions)));
        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        AtomicInteger llmCalls = new AtomicInteger();
        LlmClient llmClient = (messages, registry, context) -> {
            if (llmCalls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("confirmable", Map.of("value", "approved"), "call-1")));
            }
            return LlmCompletion.answer("Tool approved and resumed.");
        };
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                llmClient,
                toolRegistry,
                toolExecutor,
                memoryService,
                sessionManager,
                List.of(),
                3,
                "system prompt"
        );

        AgentRunResult pending = orchestrator.run("Please confirm the tool.", "hil-session");

        assertEquals(StopReason.PENDING_CONFIRMATION, pending.stopReason());
        assertEquals(0, toolExecutions.get());
        assertFalse(sessionManager.session("hil-session").pendingToolCalls().isEmpty());

        AgentRunResult resumed = orchestrator.resume("hil-session", ToolConfirmation.approved("call-1", Map.of("value", "approved")));

        assertEquals(StopReason.FINAL_ANSWER, resumed.stopReason());
        assertEquals("Tool approved and resumed.", resumed.finalAnswer());
        assertEquals(1, toolExecutions.get());
        assertTrue(sessionManager.session("hil-session").pendingToolCalls().isEmpty());
    }

    @Test
    void rejectedToolCallsAreReturnedAsExplicitFailures() {
        AtomicInteger toolExecutions = new AtomicInteger();
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new ConfirmableTool(toolExecutions)));
        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        AtomicInteger llmCalls = new AtomicInteger();
        LlmClient llmClient = (messages, registry, context) -> {
            if (llmCalls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(new LlmToolCall("confirmable", Map.of("value", "approved"), "call-1")));
            }
            boolean rejected = messages.stream().anyMatch(message ->
                    "tool".equals(message.role()) && message.content() != null && message.content().contains("Denied by user.")
            );
            return rejected
                    ? LlmCompletion.answer("Tool was denied and the agent continued safely.")
                    : LlmCompletion.answer("This should not execute.");
        };
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                llmClient,
                toolRegistry,
                toolExecutor,
                memoryService,
                sessionManager,
                List.of(),
                3,
                "system prompt"
        );

        AgentRunResult pending = orchestrator.run("Please confirm the tool.", "hil-reject");
        assertEquals(StopReason.PENDING_CONFIRMATION, pending.stopReason());

        AgentRunResult resumed = orchestrator.resume("hil-reject", ToolConfirmation.rejected("call-1", "Denied by user."));

        assertEquals(StopReason.FINAL_ANSWER, resumed.stopReason());
        assertTrue(resumed.trace().stream().anyMatch(entry -> entry.contains("Denied by user")));
        assertEquals(0, toolExecutions.get());
    }

    private static final class ConfirmableTool extends AbstractTool {
        private final AtomicInteger executions;

        private ConfirmableTool(AtomicInteger executions) {
            this.executions = executions;
        }

        @Override
        public String name() {
            return "confirmable";
        }

        @Override
        public ToolDefinition definition() {
            return new ToolDefinition(name(), "Requires explicit confirmation.", true, "Approve %s?");
        }

        @Override
        protected String executeSafely(Map<String, Object> arguments) {
            executions.incrementAndGet();
            return "confirmed:" + arguments.getOrDefault("value", "");
        }
    }
}
