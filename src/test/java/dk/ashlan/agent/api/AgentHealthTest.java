package dk.ashlan.agent.api;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.tools.CalculatorTool;
import dk.ashlan.agent.tools.ToolRegistry;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentHealthTest {
    @Test
    void readinessReportsRegisteredToolsAsUp() {
        AgentOrchestrator orchestrator = new AgentOrchestrator(null, null, null, null, 1, "") {
        };
        AgentReadinessHealthCheck check = new AgentReadinessHealthCheck(
                orchestrator,
                new ToolRegistry(List.of(new CalculatorTool()))
        );

        HealthCheckResponse response = check.call();

        assertEquals(HealthCheckResponse.Status.UP, response.getStatus());
        assertTrue(response.getData().isPresent());
        Object toolCount = response.getData().get().get("toolCount");
        assertInstanceOf(Long.class, toolCount);
        assertEquals(1L, toolCount);
    }
}
