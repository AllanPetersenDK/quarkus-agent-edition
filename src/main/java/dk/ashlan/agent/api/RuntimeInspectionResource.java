package dk.ashlan.agent.api;

import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.health.RuntimeLivenessHealthCheck;
import dk.ashlan.agent.core.ToolConfirmation;
import dk.ashlan.agent.memory.MemoryService;
import dk.ashlan.agent.memory.MemoryAwareAgentOrchestrator;
import dk.ashlan.agent.memory.SessionManager;
import dk.ashlan.agent.memory.SessionState;
import dk.ashlan.agent.memory.SessionTraceStore;
import dk.ashlan.agent.memory.TaskMemory;
import dk.ashlan.agent.api.dto.AgentStepResponse;
import dk.ashlan.agent.api.dto.AgentRunResponse;
import dk.ashlan.agent.api.dto.RuntimeSessionTraceResponse;
import dk.ashlan.agent.llm.LlmMessage;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.health.Readiness;

import java.util.List;
import java.util.Map;

@Path("/api/runtime")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Runtime Inspection", description = "Read-only runtime health, session, and memory inspection seams exposed through Swagger.")
public class RuntimeInspectionResource {
    private final AgentReadinessHealthCheck readinessHealthCheck;
    private final RuntimeLivenessHealthCheck livenessHealthCheck;
    private final SessionManager sessionManager;
    private final MemoryService memoryService;
    private final SessionTraceStore sessionTraceStore;
    private final MemoryAwareAgentOrchestrator memoryAwareAgentOrchestrator;

    public RuntimeInspectionResource(
            @Readiness AgentReadinessHealthCheck readinessHealthCheck,
            @Liveness RuntimeLivenessHealthCheck livenessHealthCheck,
            SessionManager sessionManager,
            MemoryService memoryService,
            SessionTraceStore sessionTraceStore,
            MemoryAwareAgentOrchestrator memoryAwareAgentOrchestrator
    ) {
        this.readinessHealthCheck = readinessHealthCheck;
        this.livenessHealthCheck = livenessHealthCheck;
        this.sessionManager = sessionManager;
        this.memoryService = memoryService;
        this.sessionTraceStore = sessionTraceStore;
        this.memoryAwareAgentOrchestrator = memoryAwareAgentOrchestrator;
    }

    @GET
    @Path("/health")
    @Operation(
            summary = "Inspect runtime health",
            description = "Book chapter mapping: cross-cutting runtime seam. Swagger-visible wrapper around the app's readiness and liveness checks."
    )
    @APIResponse(
            responseCode = "200",
            description = "Combined readiness and liveness snapshot.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = RuntimeHealthOverviewResponse.class))
    )
    public RuntimeHealthOverviewResponse health() {
        return new RuntimeHealthOverviewResponse(
                RuntimeHealthSnapshotResponse.from(readinessHealthCheck.call()),
                RuntimeHealthSnapshotResponse.from(livenessHealthCheck.call())
        );
    }

    @GET
    @Path("/health/ready")
    @Operation(
            summary = "Inspect readiness",
            description = "Book chapter mapping: cross-cutting runtime seam. Swagger-visible readiness check for the manual runtime and its tool registry."
    )
    @APIResponse(
            responseCode = "200",
            description = "Readiness snapshot.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = RuntimeHealthSnapshotResponse.class))
    )
    public RuntimeHealthSnapshotResponse readiness() {
        return RuntimeHealthSnapshotResponse.from(readinessHealthCheck.call());
    }

    @GET
    @Path("/health/live")
    @Operation(
            summary = "Inspect liveness",
            description = "Book chapter mapping: cross-cutting runtime seam. Swagger-visible liveness check for the companion runtime surface."
    )
    @APIResponse(
            responseCode = "200",
            description = "Liveness snapshot.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = RuntimeHealthSnapshotResponse.class))
    )
    public RuntimeHealthSnapshotResponse liveness() {
        return RuntimeHealthSnapshotResponse.from(livenessHealthCheck.call());
    }

    @GET
    @Path("/sessions/{sessionId}")
    @Operation(
            summary = "Inspect a session",
            description = "Book chapter: 6. Read-only session inspection seam backed by the existing runtime session state and conversation history."
    )
    @APIResponse(
            responseCode = "200",
            description = "Session messages and size.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = SessionInspectionResponse.class))
    )
    public SessionInspectionResponse session(
            @Parameter(description = "Session identifier used by the runtime memory layer.", required = true)
            @PathParam("sessionId") String sessionId
    ) {
        SessionState session = sessionManager.session(sessionId);
        return new SessionInspectionResponse(
                session.sessionId(),
                session.messages().stream().map(RuntimeInspectionResource::formatSessionMessage).toList(),
                session.size()
        );
    }

    @GET
    @Path("/sessions/{sessionId}/memory")
    @Operation(
            summary = "Inspect session memory",
            description = "Book chapter: 6. Read-only long-term memory inspection seam backed by the existing memory service."
    )
    @APIResponse(
            responseCode = "200",
            description = "Relevant memories for the requested session.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = MemoryInspectionResponse.class))
    )
    public MemoryInspectionResponse memory(
            @Parameter(description = "Session identifier used by the runtime memory layer.", required = true)
            @PathParam("sessionId") String sessionId,
            @Parameter(description = "Search text used to filter the stored memories.")
            @QueryParam("query") String query,
            @Parameter(description = "Maximum number of memory entries to return.")
            @QueryParam("limit") @DefaultValue("3") int limit
    ) {
        String effectiveQuery = query == null ? "" : query.trim();
        List<MemoryEntryResponse> memories = memoryService.longTermMemories(sessionId, effectiveQuery, limit).stream()
                .map(MemoryEntryResponse::from)
                .toList();
        return new MemoryInspectionResponse(sessionId, effectiveQuery, memories);
    }

    @GET
    @Path("/sessions/{sessionId}/trace")
    @Operation(
            summary = "Inspect a session trace",
            description = "Book chapter mapping: chapter 4 runtime trace inspection seam. Read-only structured view of the stored step history for a session so chapter-4 ReAct behavior can be inspected without exposing framework plumbing."
    )
    @APIResponse(
            responseCode = "200",
            description = "Structured step trace for the requested session.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = RuntimeSessionTraceResponse.class))
    )
    @APIResponse(responseCode = "404", description = "No structured trace exists for the requested session id.")
    public RuntimeSessionTraceResponse trace(
            @Parameter(description = "Session identifier used by the runtime step trace store.", required = true)
            @PathParam("sessionId") String sessionId
    ) {
        List<AgentStepResponse> steps = sessionTraceStore.load(sessionId)
                .map(list -> list.stream().map(AgentStepResponse::from).toList())
                .orElseThrow(() -> new NotFoundException("No runtime trace found for sessionId=" + sessionId));
        if (steps.isEmpty()) {
            throw new NotFoundException("No runtime trace found for sessionId=" + sessionId);
        }
        return RuntimeSessionTraceResponse.from(sessionId, steps);
    }

    @POST
    @Path("/sessions/{sessionId}/resume")
    @Operation(
            summary = "Resume a pending chapter-6 session",
            description = "Book chapter: 6. Small pause/resume seam for confirmation-gated tools. The orchestrator keeps the multi-pending confirmation logic; this endpoint only supplies the session id and a whitelist of tool confirmations."
    )
    @APIResponse(
            responseCode = "200",
            description = "Resumed run result for the requested session.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = AgentRunResponse.class))
    )
    @APIResponse(responseCode = "400", description = "No pending tool confirmation exists or the request is invalid.")
    public AgentRunResponse resume(
            @PathParam("sessionId") String sessionId,
            ResumeSessionRequest request
    ) {
        if (request == null || request.confirmations() == null) {
            throw new BadRequestException("confirmations are required");
        }
        try {
            return AgentRunResponse.from(memoryAwareAgentOrchestrator.resume(sessionId, request.confirmations().stream().map(ResumeSessionRequest.ConfirmationRequest::toConfirmation).toList()));
        } catch (IllegalStateException exception) {
            throw new BadRequestException(exception.getMessage());
        }
    }

    public record RuntimeHealthOverviewResponse(
            @Schema(description = "Readiness snapshot for the runtime companion surface.")
            RuntimeHealthSnapshotResponse readiness,
            @Schema(description = "Liveness snapshot for the runtime companion surface.")
            RuntimeHealthSnapshotResponse liveness
    ) {
    }

    public record RuntimeHealthSnapshotResponse(
            @Schema(description = "Health check name.")
            String name,
            @Schema(description = "Health status reported by the check.")
            String status,
            @Schema(description = "Supplemental health data exposed by the check.")
            Map<String, Object> data
    ) {
        static RuntimeHealthSnapshotResponse from(HealthCheckResponse response) {
            return new RuntimeHealthSnapshotResponse(
                    response.getName(),
                    response.getStatus().name(),
                    response.getData().orElse(Map.of())
            );
        }
    }

    public record SessionInspectionResponse(
            @Schema(description = "Session identifier.")
            String sessionId,
            @Schema(description = "Messages stored for the session.")
            List<String> messages,
            @Schema(description = "Number of messages in the session.")
            int messageCount
    ) {
    }

    public record MemoryInspectionResponse(
            @Schema(description = "Session identifier.")
            String sessionId,
            @Schema(description = "Query used to look up relevant memories.")
            String query,
            @Schema(description = "Relevant memories for the query.")
            List<MemoryEntryResponse> memories
    ) {
    }

    public record MemoryEntryResponse(
            @Schema(description = "Task label associated with the memory.")
            String task,
            @Schema(description = "Memory text extracted for the task.")
            String memory
    ) {
        static MemoryEntryResponse from(TaskMemory memory) {
            return new MemoryEntryResponse(memory.task(), memory.memory());
        }
    }

    public record ResumeSessionRequest(
            @Schema(description = "Whitelist of tool confirmations for the pending session.")
            List<ConfirmationRequest> confirmations
    ) {
        public record ConfirmationRequest(
                @Schema(description = "Tool call identifier to approve or reject.")
                String toolCallId,
                @Schema(description = "Whether the tool call is approved.")
                boolean approved,
                @Schema(description = "Optional modified tool arguments to use if approved.")
                Map<String, Object> arguments,
                @Schema(description = "Optional rejection reason.")
                String reason
        ) {
            ToolConfirmation toConfirmation() {
                return new ToolConfirmation(toolCallId, approved, arguments, reason);
            }
        }
    }

    private static String formatSessionMessage(LlmMessage message) {
        return switch (message.role()) {
            case "user" -> "user: " + nullToEmpty(message.content());
            case "assistant" -> message.toolCalls().isEmpty()
                    ? "assistant: " + nullToEmpty(message.content())
                    : "assistant tool calls: " + message.toolCalls().stream().map(LlmMessageToolCallView::from).map(LlmMessageToolCallView::toolName).toList();
            case "tool" -> "tool[" + nullToEmpty(message.name()) + "]: " + nullToEmpty(message.content());
            case "system" -> "system: " + nullToEmpty(message.content());
            default -> message.role() + ": " + nullToEmpty(message.content());
        };
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record LlmMessageToolCallView(String toolName) {
        static LlmMessageToolCallView from(dk.ashlan.agent.llm.LlmToolCall toolCall) {
            return new LlmMessageToolCallView(toolCall.toolName());
        }
    }
}
