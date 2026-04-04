package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.MemoryLookupRequest;
import dk.ashlan.agent.api.dto.MemoryLookupResponse;
import dk.ashlan.agent.tools.ConversationSearchTool;
import dk.ashlan.agent.tools.RecallMemoryTool;
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

import java.util.Map;

@Path("/api/runtime/memory")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Runtime Memory", description = "Swagger-visible chapter-6 direct memory retrieval seams.")
public class RuntimeMemoryResource {
    private final RecallMemoryTool recallMemoryTool;
    private final ConversationSearchTool conversationSearchTool;

    public RuntimeMemoryResource(RecallMemoryTool recallMemoryTool, ConversationSearchTool conversationSearchTool) {
        this.recallMemoryTool = recallMemoryTool;
        this.conversationSearchTool = conversationSearchTool;
    }

    @POST
    @Path("/recall")
    @Operation(
            summary = "Recall long-term memory",
            description = "Book chapter mapping: chapter 6 explicit long-term memory retrieval seam. This is the Swagger-visible companion surface for cross-session memory recall and uses the existing recall-memory tool wiring instead of hidden agent orchestration."
    )
    @RequestBody(
            description = "Session identifier and query for the long-term memory recall seam.",
            required = true,
            content = @Content(schema = @Schema(implementation = MemoryLookupRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Compact long-term memory lookup result.",
            content = @Content(schema = @Schema(implementation = MemoryLookupResponse.class))
    )
    public MemoryLookupResponse recall(@Valid MemoryLookupRequest request) {
        return invoke(recallMemoryTool, request);
    }

    @POST
    @Path("/conversation-search")
    @Operation(
            summary = "Search conversation memory",
            description = "Book chapter mapping: chapter 6 explicit session memory retrieval seam. This is the Swagger-visible companion surface for session-oriented conversation lookup and uses the existing conversation-search tool wiring instead of agent-loop indirection."
    )
    @RequestBody(
            description = "Session identifier and query for the conversation memory lookup seam.",
            required = true,
            content = @Content(schema = @Schema(implementation = MemoryLookupRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Compact conversation memory lookup result.",
            content = @Content(schema = @Schema(implementation = MemoryLookupResponse.class))
    )
    public MemoryLookupResponse conversationSearch(@Valid MemoryLookupRequest request) {
        return invoke(conversationSearchTool, request);
    }

    private MemoryLookupResponse invoke(dk.ashlan.agent.tools.Tool tool, MemoryLookupRequest request) {
        String output = tool.execute(Map.of(
                "sessionId", request.sessionId(),
                "query", request.query()
        )).output();
        return new MemoryLookupResponse(
                tool.name(),
                request.sessionId(),
                request.query(),
                output == null ? "" : output
        );
    }
}
