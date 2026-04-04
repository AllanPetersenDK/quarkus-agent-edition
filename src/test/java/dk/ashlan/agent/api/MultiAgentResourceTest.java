package dk.ashlan.agent.api;

import dk.ashlan.agent.multiagent.AgentRouter;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import dk.ashlan.agent.multiagent.CodingAgent;
import dk.ashlan.agent.multiagent.ResearchAgent;
import dk.ashlan.agent.multiagent.ReviewerAgent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiAgentResourceTest {
    @Test
    void runRoutesToSpecialistAndReportsReviewOutcome() {
        CoordinatorAgent coordinator = new CoordinatorAgent(new AgentRouter(List.of(new ResearchAgent(), new CodingAgent())), new ReviewerAgent());
        MultiAgentResource resource = new MultiAgentResource(coordinator);

        var response = resource.run(Map.of("message", "research the Quarkus agent edition"));

        assertNotNull(response.runId());
        assertNotNull(response.createdAt());
        assertEquals("research the Quarkus agent edition", response.objective());
        assertEquals("research", response.agentName());
        assertTrue(response.output().contains("Research summary"));
        assertTrue(response.review().contains("Approved"));
        assertTrue(response.approved());
        assertNotNull(response.routeReason());
        assertTrue(response.routeReason().contains("research"));
        assertTrue(response.coordinatorSummary().contains("Coordinator created"));
        assertTrue(response.traceEntries().contains("chapter9-run-start:" + response.runId()));
        assertTrue(response.traceEntries().contains("chapter9-route:research"));
        assertTrue(response.traceEntries().contains("chapter9-review:approved"));
        assertTrue(response.traceEntries().contains("chapter9-run-complete:" + response.runId()));

        var history = resource.history();
        assertEquals(1, history.size());
        assertEquals(response.runId(), history.get(0).runId());
        assertEquals(response.routeReason(), history.get(0).routeReason());
        assertEquals(response.review(), history.get(0).review());

        var reloaded = resource.historyByRunId(response.runId());
        assertEquals(response.runId(), reloaded.runId());
        assertTrue(reloaded.traceEntries().contains("chapter9-route:research"));
        assertFalse(reloaded.traceEntries().isEmpty());
    }
}
