package dk.ashlan.agent.api;

import dk.ashlan.agent.multiagent.AgentRouter;
import dk.ashlan.agent.multiagent.AgentTask;
import dk.ashlan.agent.multiagent.AgentTaskResult;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import dk.ashlan.agent.multiagent.ReviewerAgent;
import dk.ashlan.agent.multiagent.SpecialistAgent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiAgentResourceTest {
    @Test
    void runRoutesToSpecialistAndReportsReviewOutcome() {
        SpecialistAgent researchAgent = new SpecialistAgent() {
            @Override
            public String name() {
                return "research";
            }

            @Override
            public boolean supports(AgentTask task) {
                return true;
            }

            @Override
            public AgentTaskResult execute(AgentTask task) {
                return new AgentTaskResult(name(), "Research summary for: " + task.objective(), true, "Draft prepared.");
            }
        };
        CoordinatorAgent coordinator = new CoordinatorAgent(
                new AgentRouter(List.of(researchAgent)),
                new ReviewerAgent()
        );
        MultiAgentResource resource = new MultiAgentResource(coordinator);

        var response = resource.run(Map.of("message", "research the Quarkus agent edition"));

        assertEquals("research", response.agentName());
        assertTrue(response.output().contains("Research summary"));
        assertTrue(response.approved());
    }
}
