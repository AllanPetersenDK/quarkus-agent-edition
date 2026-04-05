package dk.ashlan.agent.api;

import dk.ashlan.agent.api.dto.AgentRunRequest;
import dk.ashlan.agent.api.dto.AgentRunResponse;
import dk.ashlan.agent.api.dto.AgentStructuredRunRequest;
import dk.ashlan.agent.api.dto.AgentStructuredRunResponse;
import dk.ashlan.agent.api.dto.AgentStepResponse;
import dk.ashlan.agent.core.AgentOrchestrator;
import dk.ashlan.agent.core.AgentRunResult;
import dk.ashlan.agent.core.AgentStepResult;
import dk.ashlan.agent.core.ToolConfirmation;
import dk.ashlan.agent.core.StructuredOutputAgentOrchestrator;
import dk.ashlan.agent.eval.RuntimeRunRecorder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.time.Instant;
import java.util.UUID;

@Path("/api/agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Tag(name = "Runtime API", description = "REST-exposed runtime seam for the manual agent loop and tool registry.")
public class AgentResource {
    private final AgentOrchestrator orchestrator;
    private final RuntimeRunRecorder runRecorder;
    private final StructuredOutputAgentOrchestrator structuredOutputOrchestrator = new StructuredOutputAgentOrchestrator();

    @Inject
    public AgentResource(AgentOrchestrator orchestrator) {
        this(orchestrator, null);
    }

    public AgentResource(AgentOrchestrator orchestrator, RuntimeRunRecorder runRecorder) {
        this.orchestrator = orchestrator;
        this.runRecorder = runRecorder;
    }

    @POST
    @Path("/run")
    @Operation(
            summary = "Run the manual agent loop",
            description = "Book chapter mapping: chapter 4 manual-agent core seam. REST-exposed manual agent loop that delegates to the manual AgentOrchestrator."
    )
    @RequestBody(
            description = "User message and optional session id for the runtime agent loop. If tool confirmations are supplied, sessionId must be present so the request can resume an explicit session instead of an anonymous ephemeral run.",
            required = true,
            content = @Content(schema = @Schema(implementation = AgentRunRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Runtime agent result from the manual orchestrator.",
            content = @Content(schema = @Schema(implementation = AgentRunResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid request payload.")
    public AgentRunResponse runAgent(@Valid AgentRunRequest input) {
        Instant startedAt = Instant.now();
        List<ToolConfirmation> confirmations = input.toolConfirmations() == null ? List.of() : input.toolConfirmations();
        if (!confirmations.isEmpty() && (input.sessionId() == null || input.sessionId().isBlank())) {
            throw new BadRequestException("sessionId is required when toolConfirmations are supplied");
        }
        String sessionId = effectiveSessionId(input, confirmations);
        AgentRunResult result = confirmations.isEmpty()
                ? orchestrator.run(input.message(), sessionId)
                : orchestrator.resume(sessionId, confirmations);
        AgentRunResponse response = AgentRunResponse.from(result);
        if (runRecorder != null) {
            runRecorder.recordManualRun(sessionId, input.message(), result, startedAt, Instant.now());
        }
        return response;
    }

    @POST
    @Path("/step")
    @Operation(
            summary = "Run one chapter-4 agent step",
            description = "Book chapter mapping: chapter 4 manual-loop inspection seam. Runs exactly one think/act cycle so the assistant message, tool calls, tool results, and step-local trace entries stay inspectable without exposing lower-level runtime internals."
    )
    @RequestBody(
            description = "User message and optional session id for a single chapter-4 agent step.",
            required = true,
            content = @Content(schema = @Schema(implementation = AgentRunRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Structured step result from a single manual agent cycle.",
            content = @Content(schema = @Schema(implementation = AgentStepResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid request payload.")
    public AgentStepResponse step(@Valid AgentRunRequest input) {
        Instant startedAt = Instant.now();
        AgentStepResult result = orchestrator.step(input.message(), effectiveSessionId(input, input.toolConfirmations()));
        AgentStepResponse response = AgentStepResponse.from(result);
        if (runRecorder != null) {
            runRecorder.recordManualStep(response.sessionId(), input.message(), response, startedAt, Instant.now());
        }
        return response;
    }

    @POST
    @Path("/run/structured")
    @Operation(
            summary = "Run the chapter-4 structured-output demo",
            description = "Book chapter mapping: chapter 4 structured-output seam around the manual loop. Runs one manual step and returns a single supported demo schema (`chapter4-answer`) with validation status, while keeping the outer runtime architecture explicit."
    )
    @RequestBody(
            description = "User message, optional session id, and controlled structured-output mode for the chapter-4 demo seam.",
            required = true,
            content = @Content(schema = @Schema(implementation = AgentStructuredRunRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Structured chapter-4 demo result with the raw step response attached for inspection.",
            content = @Content(schema = @Schema(implementation = AgentStructuredRunResponse.class))
    )
    @APIResponse(responseCode = "400", description = "Invalid request payload or unsupported structured-output mode.")
    public AgentStructuredRunResponse runStructured(@Valid AgentStructuredRunRequest input) {
        Instant startedAt = Instant.now();
        if (!"chapter4-answer".equals(input.mode())) {
            throw new BadRequestException("Unsupported structured-output mode: " + input.mode());
        }

        AgentStepResult step = orchestrator.step(input.message(), effectiveSessionId(input.sessionId(), input.message()));
        String rawAnswer = step.assistantMessage() != null ? step.assistantMessage() : step.finalAnswer();
        String normalizedAnswer = structuredOutputOrchestrator.normalize(rawAnswer);
        AgentStructuredRunResponse.StructuredOutputValidationStatus validationStatus =
                normalizedAnswer.isBlank()
                        ? AgentStructuredRunResponse.StructuredOutputValidationStatus.INVALID_SCHEMA
                        : AgentStructuredRunResponse.StructuredOutputValidationStatus.VALIDATED;
        AgentStructuredRunResponse.StructuredAnswerResponse structuredResult =
                normalizedAnswer.isBlank() ? null : new AgentStructuredRunResponse.StructuredAnswerResponse(normalizedAnswer);
        AgentStructuredRunResponse response = new AgentStructuredRunResponse(
                step.sessionId(),
                input.mode(),
                validationStatus,
                structuredResult,
                AgentStepResponse.from(step),
                step.isFinal() ? dk.ashlan.agent.core.StopReason.FINAL_ANSWER : null
        );
        if (runRecorder != null) {
            runRecorder.recordManualStructured(step.sessionId(), input.message(), AgentStepResponse.from(step), startedAt, Instant.now());
        }
        return response;
    }

    private String effectiveSessionId(AgentRunRequest input, List<ToolConfirmation> confirmations) {
        return effectiveSessionId(input.sessionId(), input.message(), confirmations);
    }

    private String effectiveSessionId(String sessionId, String message) {
        return effectiveSessionId(sessionId, message, List.of());
    }

    private String effectiveSessionId(String sessionId, String message, List<ToolConfirmation> confirmations) {
        boolean hasConfirmations = confirmations != null && !confirmations.isEmpty();
        if (hasConfirmations) {
            return sessionId;
        }
        if (sessionId == null || sessionId.isBlank()) {
            return "ephemeral-" + UUID.randomUUID();
        }
        return sessionId;
    }
}
