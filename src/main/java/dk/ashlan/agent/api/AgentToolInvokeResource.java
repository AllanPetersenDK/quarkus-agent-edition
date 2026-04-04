package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.ToolInvokeRequest;
import dk.ashlan.agent.api.dto.ToolInvokeResponse;
import dk.ashlan.agent.tools.JsonToolResult;
import dk.ashlan.agent.tools.Tool;
import dk.ashlan.agent.tools.ToolExecutor;
import dk.ashlan.agent.tools.ToolRegistry;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Map;

@Path("/api/agent/tools")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Tool Execution", description = "Swagger-visible chapter-3 direct tool execution seam.")
public class AgentToolInvokeResource {
    private final ToolRegistry toolRegistry;
    private final ToolExecutor toolExecutor;

    public AgentToolInvokeResource(ToolRegistry toolRegistry, ToolExecutor toolExecutor) {
        this.toolRegistry = toolRegistry;
        this.toolExecutor = toolExecutor;
    }

    @POST
    @Path("/invoke")
    @Operation(
            summary = "Invoke a runtime tool directly",
            description = "Book chapter mapping: chapter 3 tool-system execution seam. Directly invokes a registered runtime tool through the existing registry/executor pair so tool behavior can be exercised over Swagger without routing through the manual agent loop."
    )
    @RequestBody(
            description = "Runtime tool name, tool arguments, and optional session id for the direct chapter-3 tool execution seam.",
            required = true,
            content = @Content(schema = @Schema(implementation = ToolInvokeRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Tool invocation result.",
            content = @Content(schema = @Schema(implementation = ToolInvokeResponse.class))
    )
    @APIResponse(responseCode = "404", description = "Unknown tool name.")
    public ToolInvokeResponse invoke(@Valid ToolInvokeRequest request) {
        Tool tool = toolRegistry.find(request.toolName());
        if (tool == null) {
            throw new NotFoundException("Unknown tool: " + request.toolName());
        }
        Map<String, Object> arguments = request.arguments();
        JsonToolResult result = toolExecutor.execute(request.toolName(), arguments);
        String error = result.success() ? null : result.output();
        return new ToolInvokeResponse(
                request.toolName(),
                result.success(),
                result.output(),
                result.data(),
                request.sessionId(),
                error
        );
    }
}
