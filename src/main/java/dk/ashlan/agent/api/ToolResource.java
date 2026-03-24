package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.ToolSummaryResponse;
import dk.ashlan.agent.tools.Tool;
import dk.ashlan.agent.tools.ToolRegistry;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/agent")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Tools", description = "Inspect tools registered in the runtime.")
public class ToolResource {
    private final ToolRegistry toolRegistry;

    public ToolResource(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @GET
    @Path("/tools")
    @Operation(summary = "List registered tools")
    @APIResponse(
            responseCode = "200",
            description = "Registered tools",
            content = @Content(schema = @Schema(implementation = ToolSummaryResponse.class))
    )
    public List<ToolSummaryResponse> listTools() {
        return toolRegistry.tools().stream()
                .map(Tool::definition)
                .map(definition -> new ToolSummaryResponse(definition.name(), definition.description()))
                .toList();
    }
}
