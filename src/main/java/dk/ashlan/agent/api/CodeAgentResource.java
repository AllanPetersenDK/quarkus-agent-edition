package dk.ashlan.agent.api;

import dk.ashlan.agent.code.CodeAgentOrchestrator;
import dk.ashlan.agent.code.CommandResult;
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

@Path("/code-agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Internal Chapter Demo", description = "HTTP-exposed chapter demo endpoint. It is a comparison seam, not the primary runtime API.")
public class CodeAgentResource {
    private final CodeAgentOrchestrator orchestrator;

    public CodeAgentResource(CodeAgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @POST
    @Operation(
            summary = "Run the code-agent chapter demo",
            description = "Internal chapter demo endpoint that exercises the deterministic code-generation/test placeholder flow over HTTP. It does not replace the manual agent loop."
    )
    @APIResponse(
            responseCode = "200",
            description = "Code-agent demo output and the accompanying placeholder test result.",
            content = @Content(schema = @Schema(implementation = Map.class))
    )
    public Map<String, Object> run(Map<String, String> input) {
        String message = input.getOrDefault("message", "");
        CommandResult result = orchestrator.runTests();
        return Map.of(
                "response", orchestrator.run(message),
                "testExitCode", result.exitCode(),
                "testOutput", result.output()
        );
    }
}
