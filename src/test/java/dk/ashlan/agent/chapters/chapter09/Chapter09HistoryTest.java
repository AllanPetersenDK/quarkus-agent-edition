package dk.ashlan.agent.chapters.chapter09;

import dk.ashlan.agent.api.MultiAgentResource;
import dk.ashlan.agent.multiagent.AgentRouter;
import dk.ashlan.agent.multiagent.CodingAgent;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import dk.ashlan.agent.multiagent.ResearchAgent;
import dk.ashlan.agent.multiagent.ReviewerAgent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Chapter09HistoryTest {
    @Test
    void multiAgentHistoryCanBeLookedUpAfterRun() {
        CoordinatorAgent coordinator = new CoordinatorAgent(
                new AgentRouter(List.of(new ResearchAgent(), new CodingAgent())),
                new ReviewerAgent()
        );
        MultiAgentResource resource = new MultiAgentResource(coordinator);

        var research = resource.run(Map.of("message", "research the Quarkus agent edition"));
        var coding = resource.run(Map.of("message", "implement the agent routing example"));
        var thin = resource.run(Map.of("message", "x"));

        var history = resource.history();
        assertEquals(3, history.size());
        assertEquals(thin.runId(), history.get(0).runId());
        assertEquals(coding.runId(), history.get(1).runId());
        assertEquals(research.runId(), history.get(2).runId());

        var loaded = resource.historyByRunId(coding.runId());
        assertEquals("coding", loaded.agentName());
        assertTrue(loaded.output().contains("Coding plan"));
        assertTrue(loaded.routeReason().contains("coding"));
        assertTrue(loaded.review().contains("Approved"));
        assertTrue(loaded.approved());
        assertTrue(loaded.traceEntries().contains("chapter9-run-start:" + coding.runId()));
        assertTrue(loaded.traceEntries().contains("chapter9-review:approved"));
        assertNotNull(loaded.createdAt());

        var rejected = resource.historyByRunId(thin.runId());
        assertFalse(rejected.approved());
        assertTrue(rejected.review().contains("Rejected"));
        assertTrue(rejected.traceEntries().contains("chapter9-review:rejected"));
        assertEquals("research", research.agentName());
    }
}
