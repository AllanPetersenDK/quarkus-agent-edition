package dk.ashlan.agent;

import dk.ashlan.agent.multiagent.AgentRouter;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import dk.ashlan.agent.multiagent.CodingAgent;
import dk.ashlan.agent.multiagent.ResearchAgent;
import dk.ashlan.agent.multiagent.ReviewerAgent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinatorAgentTest {
    @Test
    void coordinatorRoutesToResearchSpecialist() {
        CoordinatorAgent coordinatorAgent = new CoordinatorAgent(
                new AgentRouter(List.of(new ResearchAgent(), new CodingAgent())),
                new ReviewerAgent()
        );

        var result = coordinatorAgent.run("research the Quarkus agent edition");

        assertEquals("research", result.agentName());
        assertTrue(result.output().contains("Research summary"));
        assertTrue(result.approved());
        assertTrue(result.review().contains("Approved"));
        assertTrue(result.routeReason().contains("research"));
        assertTrue(result.coordinatorSummary().contains("routed to research"));
        assertNotNull(result.routeReason());
        assertFalse(result.review().isBlank());
    }
}
