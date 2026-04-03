package dk.ashlan.agent.api;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.health.RuntimeLivenessHealthCheck;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolRegistry;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeInspectionResourceTest {
    @Test
    void runtimeInspectionReturnsHealthSessionAndMemoryViews() {
        AgentOrchestrator orchestrator = new AgentOrchestrator(null, null, null, null, 1, "") {
        };
        AgentReadinessHealthCheck readinessHealthCheck = new AgentReadinessHealthCheck(
                orchestrator,
                new ToolRegistry(List.of(new CalculatorTool()))
        );
        RuntimeLivenessHealthCheck livenessHealthCheck = new RuntimeLivenessHealthCheck();
        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(sessionManager, new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        RuntimeInspectionResource resource = new RuntimeInspectionResource(
                readinessHealthCheck,
                livenessHealthCheck,
                sessionManager,
                memoryService
        );

        memoryService.remember("session-1", "profile", "My name is Ada");

        RuntimeInspectionResource.RuntimeHealthOverviewResponse health = resource.health();
        RuntimeInspectionResource.SessionInspectionResponse session = resource.session("session-1");
        RuntimeInspectionResource.MemoryInspectionResponse memories = resource.memory("session-1", "Ada", 3);

        assertEquals(HealthCheckResponse.Status.UP.name(), health.readiness().status());
        assertEquals(HealthCheckResponse.Status.UP.name(), health.liveness().status());
        assertTrue(session.messages().contains("My name is Ada"));
        assertEquals(1, session.messageCount());
        assertTrue(memories.memories().get(0).memory().contains("Ada"));
    }
}
