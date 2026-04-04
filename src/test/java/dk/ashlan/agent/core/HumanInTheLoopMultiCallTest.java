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
import static org.junit.jupiter.api.Assertions.assertTrue;

class HumanInTheLoopMultiCallTest {
    @Test
    void multiplePendingCallsCanBeApprovedAndRejectedInOneResume() {
        AtomicInteger toolExecutions = new AtomicInteger();
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new ConfirmableTool(toolExecutions)));
        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        AtomicInteger llmCalls = new AtomicInteger();
        LlmClient llmClient = (messages, registry, context) -> {
            if (llmCalls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(
                        new LlmToolCall("confirmable", Map.of("value", "first"), "call-1"),
                        new LlmToolCall("confirmable", Map.of("value", "second"), "call-2")
                ));
            }
            boolean sawApprovedResult = messages.stream().anyMatch(message ->
                    "tool".equals(message.role()) && message.content() != null && message.content().contains("confirmed:second-updated")
            );
            boolean sawRejectedResult = messages.stream().anyMatch(message ->
                    "tool".equals(message.role()) && message.content() != null && message.content().contains("Denied by user.")
            );
            return sawApprovedResult && sawRejectedResult
                    ? LlmCompletion.answer("Processed approved and rejected pending tool calls.")
                    : LlmCompletion.answer("Missing pending tool call results.");
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

        AgentRunResult pending = orchestrator.run("Please confirm both tool calls.", "hil-multi");
        assertEquals(StopReason.PENDING_CONFIRMATION, pending.stopReason());
        assertEquals(2, sessionManager.session("hil-multi").pendingToolCalls().size());

        AgentRunResult resumed = orchestrator.resume(
                "hil-multi",
                List.of(
                        ToolConfirmation.approved("call-2", Map.of("value", "second-updated")),
                        ToolConfirmation.rejected("call-1", "Denied by user.")
                )
        );

        assertEquals(StopReason.FINAL_ANSWER, resumed.stopReason());
        assertEquals("Processed approved and rejected pending tool calls.", resumed.finalAnswer());
        assertEquals(1, toolExecutions.get());
        assertTrue(resumed.trace().stream().anyMatch(entry -> entry.contains("pending_approved:confirmable:call-2")));
        assertTrue(resumed.trace().stream().anyMatch(entry -> entry.contains("pending_rejected:confirmable:call-1")));
    }

    @Test
    void missingApprovalsAreTreatedAsRejectedPendingCalls() {
        AtomicInteger toolExecutions = new AtomicInteger();
        ToolRegistry toolRegistry = new ToolRegistry(List.of(new ConfirmableTool(toolExecutions)));
        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        ToolExecutor toolExecutor = new ToolExecutor(toolRegistry);
        AtomicInteger llmCalls = new AtomicInteger();
        LlmClient llmClient = (messages, registry, context) -> {
            if (llmCalls.getAndIncrement() == 0) {
                return LlmCompletion.toolCalls(List.of(
                        new LlmToolCall("confirmable", Map.of("value", "alpha"), "call-1"),
                        new LlmToolCall("confirmable", Map.of("value", "beta"), "call-2")
                ));
            }
            boolean sawApprovedResult = messages.stream().anyMatch(message ->
                    "tool".equals(message.role()) && message.content() != null && message.content().contains("confirmed:beta")
            );
            boolean sawImplicitRejection = messages.stream().anyMatch(message ->
                    "tool".equals(message.role()) && message.content() != null && message.content().contains("Tool call not approved.")
            );
            return sawApprovedResult && sawImplicitRejection
                    ? LlmCompletion.answer("Processed approved and implicitly rejected calls.")
                    : LlmCompletion.answer("Missing implicit rejection.");
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

        AgentRunResult pending = orchestrator.run("Please confirm both tool calls.", "hil-multi-implicit");
        assertEquals(StopReason.PENDING_CONFIRMATION, pending.stopReason());

        AgentRunResult resumed = orchestrator.resume(
                "hil-multi-implicit",
                List.of(ToolConfirmation.approved("call-2", Map.of("value", "beta")))
        );

        assertEquals(StopReason.FINAL_ANSWER, resumed.stopReason());
        assertEquals("Processed approved and implicitly rejected calls.", resumed.finalAnswer());
        assertEquals(1, toolExecutions.get());
        assertTrue(resumed.trace().stream().anyMatch(entry -> entry.contains("pending_rejected:confirmable:call-1")));
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
