package dk.ashlan.agent.api;

import dk.ashlan.agent.multiagent.AgentTaskResult;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/multi-agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Internal Chapter Demo", description = "HTTP-exposed chapter demo endpoint for the multi-agent comparison seam.")
public class MultiAgentResource {
    private final CoordinatorAgent coordinatorAgent;

    public MultiAgentResource(CoordinatorAgent coordinatorAgent) {
        this.coordinatorAgent = coordinatorAgent;
    }

    @POST
    @Operation(
            summary = "Run the multi-agent chapter demo",
            description = "Internal chapter demo endpoint that exercises the coordinator/reviewer flow over HTTP. It is a comparison seam, not the primary runtime agent path."
    )
    @APIResponse(
            responseCode = "200",
            description = "Multi-agent chapter demo result.",
            content = @Content(schema = @Schema(implementation = AgentTaskResult.class))
    )
    public AgentTaskResult run(Map<String, String> input) {
        return coordinatorAgent.run(input.getOrDefault("message", ""));
    }
}
