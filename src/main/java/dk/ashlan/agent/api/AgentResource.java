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

@Path("/api/agent")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Runtime API", description = "REST-exposed runtime seam for the manual agent loop and tool registry.")
public class AgentResource {
    private final AgentOrchestrator orchestrator;
    private final StructuredOutputAgentOrchestrator structuredOutputOrchestrator = new StructuredOutputAgentOrchestrator();

    public AgentResource(AgentOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @POST
    @Path("/run")
    @Operation(
            summary = "Run the manual agent loop",
            description = "Book chapter mapping: chapter 4 manual-agent core seam. REST-exposed manual agent loop that delegates to the manual AgentOrchestrator."
    )
    @RequestBody(
            description = "User message and optional session id for the runtime agent loop.",
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
        List<ToolConfirmation> confirmations = input.toolConfirmations() == null ? List.of() : input.toolConfirmations();
        AgentRunResult result = confirmations.isEmpty()
                ? orchestrator.run(input.message(), input.sessionId())
                : orchestrator.resume(input.sessionId(), confirmations);
        return AgentRunResponse.from(result);
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
        AgentStepResult result = orchestrator.step(input.message(), input.sessionId());
        return AgentStepResponse.from(result);
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
        if (!"chapter4-answer".equals(input.mode())) {
            throw new BadRequestException("Unsupported structured-output mode: " + input.mode());
        }

        AgentStepResult step = orchestrator.step(input.message(), input.sessionId());
        String rawAnswer = step.assistantMessage() != null ? step.assistantMessage() : step.finalAnswer();
        String normalizedAnswer = structuredOutputOrchestrator.normalize(rawAnswer);
        AgentStructuredRunResponse.StructuredOutputValidationStatus validationStatus =
                normalizedAnswer.isBlank()
                        ? AgentStructuredRunResponse.StructuredOutputValidationStatus.INVALID_SCHEMA
                        : AgentStructuredRunResponse.StructuredOutputValidationStatus.VALIDATED;
        AgentStructuredRunResponse.StructuredAnswerResponse structuredResult =
                normalizedAnswer.isBlank() ? null : new AgentStructuredRunResponse.StructuredAnswerResponse(normalizedAnswer);
        return new AgentStructuredRunResponse(
                step.sessionId(),
                input.mode(),
                validationStatus,
                structuredResult,
                AgentStepResponse.from(step),
                step.isFinal() ? dk.ashlan.agent.core.StopReason.FINAL_ANSWER : null
        );
    }
}
