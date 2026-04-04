package dk.ashlan.agent.api;

import dk.ashlan.agent.multiagent.AgentTaskResult;
import dk.ashlan.agent.multiagent.CoordinatorAgent;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;
import java.util.List;

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
            description = "Book chapter: 9. Internal chapter demo endpoint for the coordinator/reviewer flow."
    )
    @APIResponse(
            responseCode = "200",
            description = "Multi-agent chapter demo result.",
            content = @Content(schema = @Schema(implementation = AgentTaskResult.class))
    )
    public AgentTaskResult run(Map<String, String> input) {
        return coordinatorAgent.run(input.getOrDefault("message", ""));
    }

    @GET
    @Path("/history")
    @Operation(
            summary = "List multi-agent chapter history",
            description = "Return the stored chapter-9 multi-agent runs in reverse creation order so the coordinator flow stays inspectable after execution."
    )
    @APIResponse(
            responseCode = "200",
            description = "Chapter-9 multi-agent run history.",
            content = @Content(schema = @Schema(implementation = AgentTaskResult.class, type = SchemaType.ARRAY))
    )
    public List<AgentTaskResult> history() {
        return coordinatorAgent.history();
    }

    @GET
    @Path("/history/{runId}")
    @Operation(
            summary = "Inspect a single multi-agent chapter run",
            description = "Return one stored chapter-9 run so the coordinator, specialist, reviewer, and trace markers can be read back without the original response body."
    )
    @APIResponse(
            responseCode = "200",
            description = "Stored chapter-9 multi-agent run.",
            content = @Content(schema = @Schema(implementation = AgentTaskResult.class))
    )
    @APIResponse(responseCode = "404", description = "No stored chapter-9 run exists for the requested run id.")
    public AgentTaskResult historyByRunId(@PathParam("runId") String runId) {
        AgentTaskResult run = coordinatorAgent.findRun(runId);
        if (run == null) {
            throw new jakarta.ws.rs.NotFoundException("No stored chapter-9 run exists for runId=" + runId);
        }
        return run;
    }
}
