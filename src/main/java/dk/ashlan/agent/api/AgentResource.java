package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.AgentRunRequest;
import dk.ashlan.agent.api.dto.AgentRunResponse;
import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Agent", description = "Run the main agent loop.")
public class AgentResource {
    private final AgentOrchestrator orchestrator;

    public AgentResource(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @POST
    @Path("/run")
    @Operation(summary = "Run the agent loop")
    @APIResponse(
            responseCode = "200",
            description = "Agent run result",
            content = @Content(schema = @Schema(implementation = AgentRunResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid request payload")
    public AgentRunResponse runAgent(@Valid AgentRunRequest input) {
        AgentRunResult result = orchestrator.run(input.message(), input.sessionId());
        return AgentRunResponse.from(result);
    }
}
