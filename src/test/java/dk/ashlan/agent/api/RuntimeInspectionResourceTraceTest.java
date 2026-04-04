package dk.ashlan.agent.api;

import dk.ashlan.agent.core.AgentStepResult;
import dk.ashlan.agent.core.AgentTraceEntry;
import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.health.RuntimeLivenessHealthCheck;
import dk.ashlan.agent.memory.InMemorySessionTraceStore;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolRegistry;
import dk.ashlan.agent.llm.LlmToolCall;
import dk.ashlan.agent.tools.JsonToolResult;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeInspectionResourceTraceTest {
    @Test
    void traceEndpointReturnsStructuredStepsAndRejectsMissingSessions() {
        var traceStore = new InMemorySessionTraceStore();
        var readiness = new AgentReadinessHealthCheck(
                new dk.ashlan.agent.core.AgentOrchestrator(null, null, null, null, 1, "") {
                },
                new ToolRegistry(List.of(new CalculatorTool()))
        );
        var resource = new RuntimeInspectionResource(
                readiness,
                new RuntimeLivenessHealthCheck(),
                new SessionManager(),
                new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService()),
                traceStore,
                new MemoryAwareAgentOrchestrator(
                        new dk.ashlan.agent.core.AgentOrchestrator(null, null, null, null, 1, "") {
                        },
                        new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService())
                ),
                new CodeWorkspaceRegistry("target/test-chapter8-workspaces")
        );

        assertThrows(NotFoundException.class, () -> resource.trace("missing-session"));

        traceStore.append(new AgentStepResult(
                "session-1",
                1,
                "answer: 100",
                List.of(new LlmToolCall("calculator", Map.of("expression", "25 * 4"), "call-1")),
                List.of(JsonToolResult.success("calculator", "100")),
                "answer: 100",
                true,
                List.of(
                        new AgentTraceEntry("step", "iteration:1"),
                        new AgentTraceEntry("tool-call", "calculator"),
                        new AgentTraceEntry("tool-result", "100"),
                        new AgentTraceEntry("assistant-message", "answer: 100")
                )
        ));

        RuntimeInspectionResource.RuntimeHealthOverviewResponse ignored = resource.health();
        assertNotNull(ignored);

        var response = resource.trace("session-1");
        assertEquals("session-1", response.sessionId());
        assertEquals(1, response.steps().size());
        assertEquals(1, response.steps().get(0).stepNumber());
        assertEquals("calculator", response.steps().get(0).toolCalls().get(0).toolName());
        assertEquals("100", response.steps().get(0).toolResults().get(0).output());
        assertEquals("answer: 100", response.finalAnswer());
        assertEquals(dk.ashlan.agent.core.StopReason.FINAL_ANSWER, response.stopReason());
        assertTrue(response.steps().get(0).isFinal());
    }
}
