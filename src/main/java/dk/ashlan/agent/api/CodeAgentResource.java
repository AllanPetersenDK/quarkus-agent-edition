package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.CodeAgentRunRequest;
import dk.ashlan.agent.api.dto.CodeAgentRunResponse;
import dk.ashlan.agent.code.CodeAgentOrchestrator;
import jakarta.validation.Valid;
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

@Path("/api/code-agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Internal Chapter Demo", description = "HTTP-exposed chapter demo endpoint. It is a comparison seam, not the primary runtime API.")
public class CodeAgentResource {
    private final CodeAgentOrchestrator orchestrator;

    public CodeAgentResource(CodeAgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @POST
    @Path("/run")
    @Operation(
            summary = "Run the code-agent chapter demo",
            description = "Book chapter: 8. Internal chapter 8 companion endpoint for the constrained workspace code-agent flow. The run creates workspace-local artifacts, registers a session-scoped generated tool, and records stable Chapter 8 trace markers."
    )
    @APIResponse(
            responseCode = "200",
            description = "Code-agent companion output and the accompanying placeholder test result.",
            content = @Content(schema = @Schema(implementation = CodeAgentRunResponse.class))
    )
    public CodeAgentRunResponse run(@Valid CodeAgentRunRequest input) {
        return CodeAgentRunResponse.from(orchestrator.run(input.sessionId(), input.message()));
    }
}
