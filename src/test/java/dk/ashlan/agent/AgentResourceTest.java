package dk.ashlan.agent;

import dk.ashlan.agent.api.AgentResource;
import dk.ashlan.agent.api.dto.AgentRunRequest;
import dk.ashlan.agent.api.dto.AgentRunResponse;
import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.StopReason;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentResourceTest {
    @Test
    void runAgentReturnsResponseFromOrchestrator() {
        AgentOrchestrator orchestrator = new AgentOrchestrator(null, null, null, null, 1, "") {
            @Override
            public AgentRunResult run(String message, String sessionId) {
                return new AgentRunResult(
                        "The result is 100",
                        StopReason.FINAL_ANSWER,
                        2,
                        List.of("iteration:1", "tool:calculator:100", "answer:The result is 100")
                );
            }
        };
        AgentResource resource = new AgentResource(orchestrator);

        AgentRunResponse response = resource.runAgent(new AgentRunRequest("What is 25 * 4?", "default"));

        assertEquals("The result is 100", response.answer());
        assertEquals(StopReason.FINAL_ANSWER, response.stopReason());
        assertEquals(2, response.iterations());
        assertTrue(response.trace().contains("tool:calculator:100"));
    }
}
