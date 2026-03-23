package dk.ashlan.agent;

import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.AgentRunner;
import dk.ashlan.agent.core.StopReason;
import dk.ashlan.agent.planning.PlannedAgentOrchestrator;
import dk.ashlan.agent.planning.PlannerService;
import dk.ashlan.agent.planning.ReflectionService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannedAgentOrchestratorTest {
    @Test
    void reflectionCanTriggerImprovementLoop() {
        AgentRunner runner = message -> new AgentRunResult("short", StopReason.FINAL_ANSWER, 1, java.util.List.of(message));
        PlannedAgentOrchestrator orchestrator = new PlannedAgentOrchestrator(new PlannerService(), new ReflectionService(), runner);

        AgentRunResult result = orchestrator.run("Explain Quarkus agents");

        assertEquals(StopReason.REFLECTION_REJECTED, result.stopReason());
        assertTrue(result.finalAnswer().contains("short"));
    }
}
