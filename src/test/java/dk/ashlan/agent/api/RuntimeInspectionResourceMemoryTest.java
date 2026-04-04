package dk.ashlan.agent.api;

import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.health.RuntimeLivenessHealthCheck;
import dk.ashlan.agent.memory.InMemoryTaskMemoryStore;
import dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator;
import dk.ashlan.agent.memory.MemoryExtractionService;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.SessionTraceStore;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.eval.RuntimeRunHistoryStore;
import dk.ashlan.agent.tools.ToolRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeInspectionResourceMemoryTest {
    @Test
    void memoryInspectionExposesStructuredStorageFields() {
        SessionManager sessionManager = new SessionManager();
        MemoryService memoryService = new MemoryService(sessionManager, new InMemoryTaskMemoryStore(), new MemoryExtractionService());
        memoryService.remember("session-1", "goal", "Remember that my favorite database is PostgreSQL.");

        RuntimeInspectionResource resource = new RuntimeInspectionResource(
                new AgentReadinessHealthCheck(new dk.ashlan.agent.core.AgentOrchestrator(null, null, null, null, 1, "") {
                }, new ToolRegistry(List.of())),
                new RuntimeLivenessHealthCheck(),
                sessionManager,
                memoryService,
                new SessionTraceStore() {
                    @Override
                    public java.util.Optional<List<dk.ashlan.agent.core.AgentStepResult>> load(String sessionId) {
                        return java.util.Optional.empty();
                    }

                    @Override
                    public void append(dk.ashlan.agent.core.AgentStepResult stepResult) {
                    }
                },
                new MemoryAwareAgentOrchestrator(
                        new dk.ashlan.agent.core.AgentOrchestrator(null, null, null, null, 1, "") {
                        },
                        memoryService
                ),
                new CodeWorkspaceRegistry("target/test-chapter8-workspaces"),
                new RuntimeRunHistoryStore()
        );

        RuntimeInspectionResource.MemoryInspectionResponse response = resource.memory("session-1", "PostgreSQL", 3);

        assertEquals("session-1", response.sessionId());
        assertEquals("PostgreSQL", response.query());
        assertEquals(1, response.memories().size());
        assertNotNull(response.memories().get(0).summary());
        assertNotNull(response.memories().get(0).result());
        assertTrue(response.memories().get(0).memory().contains("PostgreSQL"));
    }
}
