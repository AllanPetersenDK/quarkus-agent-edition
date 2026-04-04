package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.ContextOptimizeRequest;
import dk.ashlan.agent.api.dto.ContextOptimizeResponse;
import dk.ashlan.agent.core.ContextOptimizationResult;
import dk.ashlan.agent.core.ContextOptimizer;
import dk.ashlan.agent.llm.LlmMessage;
import dk.ashlan.agent.llm.LlmRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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

import java.util.List;

@Path("/api/runtime/context")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Runtime Context", description = "Swagger-visible chapter-6 request projection inspection seam.")
public class RuntimeContextResource {
    private final ContextOptimizer contextOptimizer;

    public RuntimeContextResource(ContextOptimizer contextOptimizer) {
        this.contextOptimizer = contextOptimizer;
    }

    @POST
    @Path("/optimize")
    @Operation(
            summary = "Optimize a request projection",
            description = "Book chapter mapping: chapter 6 request-time context optimization inspection seam. This endpoint shows how the existing optimizer projects a request before any LLM call, without mutating session state or running the agent loop."
    )
    @RequestBody(
            description = "Messages to project through the existing chapter-6 context optimizer.",
            required = true,
            content = @Content(schema = @Schema(implementation = ContextOptimizeRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Token counts, selected strategy, and the original/projected message sets.",
            content = @Content(schema = @Schema(implementation = ContextOptimizeResponse.class))
    )
    public ContextOptimizeResponse optimize(@Valid ContextOptimizeRequest request) {
        List<LlmMessage> messages = request.messages().stream()
                .map(this::toMessage)
                .toList();
        ContextOptimizationResult result = contextOptimizer.optimize(new LlmRequest(messages));
        return buildResponse(request.messages(), result);
    }

    @POST
    @Path("/sliding-window")
    @Operation(
            summary = "Preview a sliding-window projection",
            description = "Book chapter mapping: chapter 6 sliding-window inspection seam. This endpoint shows the existing sliding-window strategy in isolation, so the chapter-6 context hierarchy is visible without mutating session state or running the agent loop."
    )
    @RequestBody(
            description = "Messages to project through the existing chapter-6 sliding-window strategy.",
            required = true,
            content = @Content(schema = @Schema(implementation = ContextOptimizeRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Token counts, sliding-window strategy, and the original/projected message sets.",
            content = @Content(schema = @Schema(implementation = ContextOptimizeResponse.class))
    )
    public ContextOptimizeResponse slidingWindow(@Valid ContextOptimizeRequest request) {
        List<LlmMessage> messages = request.messages().stream()
                .map(this::toMessage)
                .toList();
        ContextOptimizationResult result = contextOptimizer.previewSlidingWindow(new LlmRequest(messages));
        return buildResponse(request.messages(), result);
    }

    private ContextOptimizeResponse buildResponse(List<ContextOptimizeRequest.ContextOptimizeMessage> originalMessages, ContextOptimizationResult result) {
        return new ContextOptimizeResponse(
                result.originalTokenCount(),
                result.projectedTokenCount(),
                result.strategy(),
                result.changed(),
                originalMessages,
                result.messages().stream().map(this::toDto).toList()
        );
    }

    private LlmMessage toMessage(ContextOptimizeRequest.ContextOptimizeMessage message) {
        return new LlmMessage(
                message.role(),
                message.content(),
                message.name(),
                message.toolCallId(),
                List.of()
        );
    }

    private ContextOptimizeRequest.ContextOptimizeMessage toDto(LlmMessage message) {
        return new ContextOptimizeRequest.ContextOptimizeMessage(
                message.role(),
                message.content(),
                message.name(),
                message.toolCallId()
        );
    }
}
