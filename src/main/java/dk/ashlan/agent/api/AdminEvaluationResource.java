package dk.ashlan.agent.api;

import dk.ashlan.agent.eval.AgentTrace;
import dk.ashlan.agent.eval.AgentTraceService;
import dk.ashlan.agent.eval.EvalCase;
import dk.ashlan.agent.eval.EvalResult;
import dk.ashlan.agent.eval.EvaluationRunner;
import dk.ashlan.agent.eval.RunMetrics;
import dk.ashlan.agent.eval.gaia.GaiaEvaluationRunner;
import dk.ashlan.agent.eval.gaia.GaiaValidationRequest;
import dk.ashlan.agent.eval.gaia.GaiaValidationResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Path("/admin/evaluations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Internal Evaluation", description = "HTTP-exposed evaluation harness and trace inspection seam for chapter ten comparison runs.")
public class AdminEvaluationResource {
    private final EvaluationRunner evaluationRunner;
    private final AgentTraceService traceService;
    private final GaiaEvaluationRunner gaiaEvaluationRunner;

    public AdminEvaluationResource(EvaluationRunner evaluationRunner, AgentTraceService traceService, GaiaEvaluationRunner gaiaEvaluationRunner) {
        this.evaluationRunner = evaluationRunner;
        this.traceService = traceService;
        this.gaiaEvaluationRunner = gaiaEvaluationRunner;
    }

    @POST
    @Operation(
            summary = "Run evaluation cases",
            description = "Book chapter: 10. Internal admin endpoint that runs evaluation cases and returns results and metrics."
    )
    @RequestBody(
            description = "Evaluation cases to execute.",
            required = true,
            content = @Content(schema = @Schema(implementation = EvalCase.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Evaluation results and aggregate metrics.",
            content = @Content(schema = @Schema(implementation = Map.class))
    )
    public Map<String, Object> run(List<EvalCase> cases) {
        long startedAt = System.nanoTime();
        List<EvalResult> results = evaluationRunner.run(cases);
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        RunMetrics metrics = evaluationRunner.metrics(results, durationMillis);
        return Map.of("results", results, "metrics", metrics);
    }

    @POST
    @Path("/gaia")
    @Operation(
            summary = "Run GAIA starter validation",
            description = "Book chapter: 10. Starter GAIA validation flow for Level 1 cases without attachments. It exercises the existing manual runtime agent only and keeps attachment-heavy GAIA cases out of scope."
    )
    @RequestBody(
            description = "Subset limit for the GAIA starter validation run.",
            required = true,
            content = @Content(schema = @Schema(implementation = GaiaValidationRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Filtered GAIA validation results and summary.",
            content = @Content(schema = @Schema(implementation = GaiaValidationResponse.class))
    )
    public GaiaValidationResponse runGaia(GaiaValidationRequest request) {
        int limit = request == null ? 0 : request.effectiveLimit(10);
        return gaiaEvaluationRunner.run(limit);
    }

    @GET
    @Path("/{caseId}")
    @Operation(
            summary = "Inspect an evaluation trace",
            description = "Book chapter: 10. Read-only trace inspection seam for the most recent run of a named evaluation case."
    )
    @APIResponse(
            responseCode = "200",
            description = "Stored evaluation trace.",
            content = @Content(schema = @Schema(implementation = EvaluationTraceResponse.class))
    )
    @APIResponse(responseCode = "404", description = "No evaluation trace exists for the requested case id.")
    public EvaluationTraceResponse trace(
            @Parameter(description = "Evaluation case identifier.", required = true)
            @PathParam("caseId") String caseId
    ) {
        AgentTrace trace = traceService.get(caseId);
        if (trace == null) {
            throw new NotFoundException("No evaluation trace found for caseId=" + caseId);
        }
        return new EvaluationTraceResponse(trace.caseId(), trace.events());
    }

    public record EvaluationTraceResponse(
            @Schema(description = "Evaluation case identifier.")
            String caseId,
            @Schema(description = "Stored trace events for the evaluation run.")
            List<String> events
    ) {
    }
}
