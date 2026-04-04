package dk.ashlan.agent.api;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.core.ToolConfirmation;
import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.health.RuntimeLivenessHealthCheck;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.InMemorySessionTraceStore;
import dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeInspectionResourceResumeTest {
    @Test
    void resumeEndpointPassesConfirmationListToMemoryAwareOrchestrator() {
        AgentOrchestrator orchestrator = new AgentOrchestrator(null, null, null, null, 1, "") {
            @Override
            public AgentRunResult resume(String sessionId, List<ToolConfirmation> confirmations) {
                return new AgentRunResult(
                        "resumed",
                        StopReason.FINAL_ANSWER,
                        confirmations.size(),
                        List.of("pending_approved:confirmation-demo:call-1")
                );
            }
        };
        MemoryAwareAgentOrchestrator memoryAwareAgentOrchestrator = new MemoryAwareAgentOrchestrator(
                orchestrator,
                new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService())
        );
        RuntimeInspectionResource resource = new RuntimeInspectionResource(
                new AgentReadinessHealthCheck(
                        new AgentOrchestrator(null, new ToolRegistry(List.of()), null, null, 1, "") {
                        },
                        new ToolRegistry(List.of())
                ),
                new RuntimeLivenessHealthCheck(),
                new SessionManager(),
                new MemoryService(new SessionManager(), new InMemoryTaskMemoryStore(), new MemoryExtractionService()),
                new InMemorySessionTraceStore(),
                memoryAwareAgentOrchestrator,
                new CodeWorkspaceRegistry("target/test-chapter8-workspaces")
        );

        RuntimeInspectionResource.ResumeSessionRequest request = new RuntimeInspectionResource.ResumeSessionRequest(
                List.of(new RuntimeInspectionResource.ResumeSessionRequest.ConfirmationRequest(
                        "call-1",
                        true,
                        Map.of("topic", "chapter 6"),
                        null
                ))
        );

        var response = resource.resume("session-1", request);

        assertEquals("resumed", response.answer());
        assertEquals(StopReason.FINAL_ANSWER, response.stopReason());
        assertEquals(1, response.iterations());
        assertEquals(1, response.trace().size());
    }
}
