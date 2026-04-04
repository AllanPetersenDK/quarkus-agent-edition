package dk.ashlan.agent.api;

import dk.ashlan.agent.health.AgentReadinessHealthCheck;
import dk.ashlan.agent.health.RuntimeLivenessHealthCheck;
import dk.ashlan.agent.api.dto.CodeWorkspaceFilesResponse;
import dk.ashlan.agent.api.dto.CodeWorkspaceInspectionResponse;
import dk.ashlan.agent.api.dto.GeneratedWorkspaceToolInvokeRequest;
import dk.ashlan.agent.api.dto.GeneratedWorkspaceToolInvokeResponse;
import dk.ashlan.agent.api.dto.GeneratedWorkspaceToolResponse;
import dk.ashlan.agent.api.dto.GeneratedWorkspaceToolsResponse;
import dk.ashlan.agent.core.AgentStepResult;
import dk.ashlan.agent.core.ToolConfirmation;
import dk.ashlan.agent.code.CodeWorkspaceRegistry;
import dk.ashlan.agent.code.CodeWorkspaceSession;
import dk.ashlan.agent.planning.ExecutionPlan;
import dk.ashlan.agent.planning.PlanStep;
import dk.ashlan.agent.planning.Chapter7ReflectionState;
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
import jakarta.validation.Valid;
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
    private final CodeWorkspaceRegistry codeWorkspaceRegistry;

    public RuntimeInspectionResource(
            @Readiness AgentReadinessHealthCheck readinessHealthCheck,
            @Liveness RuntimeLivenessHealthCheck livenessHealthCheck,
            SessionManager sessionManager,
            MemoryService memoryService,
            SessionTraceStore sessionTraceStore,
            MemoryAwareAgentOrchestrator memoryAwareAgentOrchestrator,
            CodeWorkspaceRegistry codeWorkspaceRegistry
    ) {
        this.readinessHealthCheck = readinessHealthCheck;
        this.livenessHealthCheck = livenessHealthCheck;
        this.sessionManager = sessionManager;
        this.memoryService = memoryService;
        this.sessionTraceStore = sessionTraceStore;
        this.memoryAwareAgentOrchestrator = memoryAwareAgentOrchestrator;
        this.codeWorkspaceRegistry = codeWorkspaceRegistry;
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
    @Path("/sessions/{sessionId}/plan")
    @Operation(
            summary = "Inspect a chapter-7 plan",
            description = "Book chapter: 7. Read-only chapter-7 planning inspection seam backed by the current session state so the active task plan stays visible in Swagger without introducing a workflow engine."
    )
    @APIResponse(
            responseCode = "200",
            description = "Current plan state for the requested session.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = SessionPlanInspectionResponse.class))
    )
    public SessionPlanInspectionResponse plan(
            @Parameter(description = "Session identifier used by the runtime chapter-7 planning state.", required = true)
            @PathParam("sessionId") String sessionId
    ) {
        SessionState session = sessionManager.session(sessionId);
        return SessionPlanInspectionResponse.from(sessionId, session.chapter7Plan());
    }

    @GET
    @Path("/sessions/{sessionId}/reflection")
    @Operation(
            summary = "Inspect a chapter-7 reflection",
            description = "Book chapter: 7. Read-only chapter-7 reflection inspection seam backed by the current session state so review, synthesis, and replan signals are visible in Swagger."
    )
    @APIResponse(
            responseCode = "200",
            description = "Current reflection state for the requested session.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = SessionReflectionInspectionResponse.class))
    )
    public SessionReflectionInspectionResponse reflection(
            @Parameter(description = "Session identifier used by the runtime chapter-7 reflection state.", required = true)
            @PathParam("sessionId") String sessionId
    ) {
        SessionState session = sessionManager.session(sessionId);
        Chapter7ReflectionState reflection = session.chapter7Reflection();
        if (!hasMeaningfulReflection(reflection)) {
            reflection = reflectionFromTrace(sessionId);
        }
        return SessionReflectionInspectionResponse.from(sessionId, reflection);
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

    @GET
    @Path("/sessions/{sessionId}/workspace")
    @Operation(
            summary = "Inspect a chapter-8 workspace",
            description = "Book chapter: 8. Read-only workspace inspection seam backed by the session-scoped Chapter 8 workspace registry so the current workspace root, timestamps, and file count stay visible in Swagger without introducing a workflow platform."
    )
    @APIResponse(
            responseCode = "200",
            description = "Current Chapter 8 workspace state for the requested session.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = CodeWorkspaceInspectionResponse.class))
    )
    public CodeWorkspaceInspectionResponse workspace(
            @Parameter(description = "Session identifier used for the Chapter 8 workspace.", required = true)
            @PathParam("sessionId") String sessionId
    ) {
        CodeWorkspaceSession session = codeWorkspaceRegistry.session(sessionId);
        return new CodeWorkspaceInspectionResponse(
                session.sessionId(),
                session.workspaceId(),
                session.workspaceRoot(),
                session.createdAt(),
                session.updatedAt(),
                session.fileCount(),
                session.generatedTools().size(),
                session.lastRequest(),
                session.traceMarkers()
        );
    }

    @GET
    @Path("/sessions/{sessionId}/workspace/files")
    @Operation(
            summary = "Inspect Chapter 8 workspace files",
            description = "Book chapter: 8. Workspace file listing seam for the Chapter 8 companion runtime so the file round-trip behavior stays explicit and reviewable."
    )
    @APIResponse(
            responseCode = "200",
            description = "Workspace-relative file listing for the requested session.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = CodeWorkspaceFilesResponse.class))
    )
    public CodeWorkspaceFilesResponse workspaceFiles(
            @Parameter(description = "Session identifier used for the Chapter 8 workspace.", required = true)
            @PathParam("sessionId") String sessionId
    ) {
        CodeWorkspaceSession session = codeWorkspaceRegistry.session(sessionId);
        return new CodeWorkspaceFilesResponse(
                session.sessionId(),
                session.workspaceId(),
                session.workspaceRoot(),
                session.files(),
                session.traceMarkers()
        );
    }

    @GET
    @Path("/sessions/{sessionId}/generated-tools")
    @Operation(
            summary = "Inspect Chapter 8 generated tools",
            description = "Book chapter: 8. Read-only generated-tool registry seam for the session-scoped Chapter 8 companion runtime."
    )
    @APIResponse(
            responseCode = "200",
            description = "Session-scoped generated tools for the requested session.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = GeneratedWorkspaceToolsResponse.class))
    )
    public GeneratedWorkspaceToolsResponse generatedTools(
            @Parameter(description = "Session identifier used for the Chapter 8 workspace.", required = true)
            @PathParam("sessionId") String sessionId
    ) {
        CodeWorkspaceSession session = codeWorkspaceRegistry.session(sessionId);
        return new GeneratedWorkspaceToolsResponse(
                session.sessionId(),
                session.workspaceId(),
                session.generatedTools().stream().map(GeneratedWorkspaceToolResponse::from).toList(),
                session.traceMarkers()
        );
    }

    @POST
    @Path("/sessions/{sessionId}/generated-tools/invoke")
    @Operation(
            summary = "Invoke a Chapter 8 generated tool",
            description = "Book chapter: 8. Direct invocation seam for the session-scoped generated tools created by the Chapter 8 companion runtime."
    )
    @APIResponse(
            responseCode = "200",
            description = "Generated-tool invocation result.",
            content = @org.eclipse.microprofile.openapi.annotations.media.Content(schema = @Schema(implementation = GeneratedWorkspaceToolInvokeResponse.class))
    )
    public GeneratedWorkspaceToolInvokeResponse generatedToolInvoke(
            @Parameter(description = "Session identifier used for the Chapter 8 workspace.", required = true)
            @PathParam("sessionId") String sessionId,
            @Valid GeneratedWorkspaceToolInvokeRequest request
    ) {
        CodeWorkspaceSession session = codeWorkspaceRegistry.session(sessionId);
        var result = session.invokeGeneratedTool(request.toolName(), request.arguments());
        return new GeneratedWorkspaceToolInvokeResponse(
                session.sessionId(),
                session.workspaceId(),
                request.toolName(),
                result.success(),
                result.output(),
                result.data(),
                session.traceMarkers()
        );
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

    public record SessionPlanInspectionResponse(
            @Schema(description = "Session identifier.")
            String sessionId,
            @Schema(description = "Current task-plan goal.")
            String goal,
            @Schema(description = "Plan status for the session.")
            String status,
            @Schema(description = "Current chapter-7 task steps.")
            List<PlanStepResponse> steps,
            @Schema(description = "Next active task step, if one exists.", nullable = true)
            PlanStepResponse nextActiveStep
    ) {
        static SessionPlanInspectionResponse from(String sessionId, ExecutionPlan plan) {
            if (!hasMeaningfulPlan(plan)) {
                return new SessionPlanInspectionResponse(sessionId, "", "missing", List.of(), null);
            }
            List<PlanStepResponse> steps = plan.steps() == null ? List.of() : plan.steps().stream().map(PlanStepResponse::from).toList();
            PlanStepResponse nextActiveStep = plan.nextActiveStep() == null ? null : PlanStepResponse.from(plan.nextActiveStep());
            return new SessionPlanInspectionResponse(sessionId, plan.goal(), "active", steps, nextActiveStep);
        }
    }

    public record SessionReflectionInspectionResponse(
            @Schema(description = "Session identifier.")
            String sessionId,
            @Schema(description = "Reflection status for the session.")
            String status,
            @Schema(description = "Reflection mode.", nullable = true)
            String mode,
            @Schema(description = "Reflection analysis text.", nullable = true)
            String analysis,
            @Schema(description = "Whether the reflection accepted the current answer.")
            boolean accepted,
            @Schema(description = "Whether a replan is needed.")
            boolean needReplan,
            @Schema(description = "Whether the answer is ready to be finalized.")
            boolean readyToAnswer,
            @Schema(description = "Alternative direction suggested by the reflection.", nullable = true)
            String alternativeDirection,
            @Schema(description = "Next step suggested by the reflection.", nullable = true)
            String nextStep,
            @Schema(description = "Summary text captured for the reflection.", nullable = true)
            String summary
    ) {
        static SessionReflectionInspectionResponse from(String sessionId, Chapter7ReflectionState reflection) {
            if (!hasMeaningfulReflection(reflection)) {
                return new SessionReflectionInspectionResponse(sessionId, "missing", "", "", false, false, false, "", "", "");
            }
            return new SessionReflectionInspectionResponse(
                    sessionId,
                    "available",
                    reflection.mode(),
                    reflection.analysis(),
                    reflection.accepted(),
                    reflection.needReplan(),
                    reflection.readyToAnswer(),
                    reflection.alternativeDirection(),
                    reflection.nextStep(),
                    reflection.summary()
            );
        }
    }

    private static boolean hasMeaningfulPlan(ExecutionPlan plan) {
        if (plan == null) {
            return false;
        }
        boolean hasGoal = plan.goal() != null && !plan.goal().isBlank();
        boolean hasSteps = plan.steps() != null && !plan.steps().isEmpty();
        return hasGoal || hasSteps;
    }

    private static boolean hasMeaningfulReflection(Chapter7ReflectionState reflection) {
        if (reflection == null) {
            return false;
        }
        boolean hasText = !reflection.mode().isBlank()
                || !reflection.analysis().isBlank()
                || !reflection.alternativeDirection().isBlank()
                || !reflection.nextStep().isBlank()
                || !reflection.summary().isBlank();
        return hasText || reflection.accepted() || reflection.needReplan() || reflection.readyToAnswer();
    }

    private Chapter7ReflectionState reflectionFromTrace(String sessionId) {
        if (sessionTraceStore == null) {
            return null;
        }
        return sessionTraceStore.load(sessionId)
                .map(steps -> {
                    for (int i = steps.size() - 1; i >= 0; i--) {
                        Chapter7ReflectionState reflection = reflectionFromStep(steps.get(i));
                        if (reflection != null) {
                            return reflection;
                        }
                    }
                    return null;
                })
                .orElse(null);
    }

    private Chapter7ReflectionState reflectionFromStep(AgentStepResult step) {
        if (step == null || step.traceEntries() == null || step.traceEntries().isEmpty()) {
            return null;
        }
        String reflectionMessage = null;
        boolean replan = false;
        for (int i = step.traceEntries().size() - 1; i >= 0; i--) {
            var entry = step.traceEntries().get(i);
            if (entry == null || entry.kind() == null) {
                continue;
            }
            if ("replan".equals(entry.kind())) {
                replan = true;
            } else if ("reflection".equals(entry.kind()) && reflectionMessage == null) {
                reflectionMessage = entry.message();
            }
        }
        if (reflectionMessage == null || reflectionMessage.isBlank()) {
            return null;
        }
        String normalized = reflectionMessage.trim();
        String mode = normalized.contains("ERROR ANALYSIS") ? "error_analysis" : normalized.contains("PROGRESS REVIEW") ? "progress_review" : "self_check";
        boolean needReplan = replan || normalized.contains("REPLAN NEEDED");
        String analysis = normalized.contains(":") ? normalized.substring(normalized.indexOf(':') + 1).trim() : normalized;
        return new Chapter7ReflectionState(
                mode,
                analysis,
                !needReplan,
                needReplan,
                !needReplan && !analysis.isBlank(),
                needReplan ? "Switch method and retry with the corrected input." : "",
                needReplan ? "Revise the plan around the failure cause." : "",
                normalized
        );
    }

    public record PlanStepResponse(
            @Schema(description = "Task order in the plan.")
            int order,
            @Schema(description = "Task description.")
            String content,
            @Schema(description = "Task status.")
            String status,
            @Schema(description = "Completion cue for the task.", nullable = true)
            String doneWhen,
            @Schema(description = "Optional notes for the task.", nullable = true)
            String notes
    ) {
        static PlanStepResponse from(PlanStep step) {
            return new PlanStepResponse(
                    step.order(),
                    step.description(),
                    step.status().name().toLowerCase(),
                    step.doneWhen(),
                    step.notes()
            );
        }
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
            @Schema(description = "Structured problem statement when available.")
            String problem,
            @Schema(description = "Structured summary when available.")
            String summary,
            @Schema(description = "Structured approach description when available.")
            String approach,
            @Schema(description = "Structured result text when available.")
            String result,
            @Schema(description = "Whether the memory was marked correct, if known.")
            Boolean correct,
            @Schema(description = "Structured error analysis when available.")
            String errorAnalysis,
            @Schema(description = "Memory text extracted for the task.")
            String memory
    ) {
        static MemoryEntryResponse from(TaskMemory memory) {
            return new MemoryEntryResponse(
                    memory.task(),
                    memory.problem(),
                    memory.summary(),
                    memory.approach(),
                    memory.result(),
                    memory.correct(),
                    memory.errorAnalysis(),
                    memory.memory()
            );
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
