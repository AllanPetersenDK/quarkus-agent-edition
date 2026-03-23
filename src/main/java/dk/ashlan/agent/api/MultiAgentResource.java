package dk.ashlan.agent.api;

import dk.ashlan.agent.multiagent.AgentTaskResult;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/multi-agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MultiAgentResource {
    private final CoordinatorAgent coordinatorAgent;

    public MultiAgentResource(CoordinatorAgent coordinatorAgent) {
        this.coordinatorAgent = coordinatorAgent;
    }

    @POST
    public AgentTaskResult run(Map<String, String> input) {
        return coordinatorAgent.run(input.getOrDefault("message", ""));
    }
}
