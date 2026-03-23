package dk.ashlan.agent.api;

import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AgentResource {
    private final AgentOrchestrator orchestrator;

    public AgentResource(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @POST
    public AgentRunResult runAgent(Map<String, String> input) {
        return orchestrator.run(input.getOrDefault("message", ""), input.getOrDefault("sessionId", "default"));
    }
}
