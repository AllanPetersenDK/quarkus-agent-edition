package dk.ashlan.agent.api;

import dk.ashlan.agent.eval.AgentTrace;
import dk.ashlan.agent.eval.AgentTraceService;
import dk.ashlan.agent.eval.EvalCase;
import dk.ashlan.agent.eval.EvalResult;
import dk.ashlan.agent.eval.EvaluationRunner;
import dk.ashlan.agent.eval.RunMetrics;
import dk.ashlan.agent.eval.RuntimeRunRecorder;
import dk.ashlan.agent.eval.Chapter10EvalRunRequest;
import dk.ashlan.agent.eval.Chapter10EvalRunResult;
import dk.ashlan.agent.eval.Chapter10EvaluationService;
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
@Tag(name = "Internal Evaluation", description = "HTTP-exposed generic evaluation harness and trace inspection seam for chapter ten comparison runs.")
public class AdminEvaluationResource {
    private final EvaluationRunner evaluationRunner;
    private final AgentTraceService traceService;
    private final RuntimeRunRecorder runRecorder;
    private final Chapter10EvaluationService chapter10EvaluationService;

    public AdminEvaluationResource(EvaluationRunner evaluationRunner, AgentTraceService traceService, RuntimeRunRecorder runRecorder, Chapter10EvaluationService chapter10EvaluationService) {
        this.evaluationRunner = evaluationRunner;
        this.traceService = traceService;
        this.runRecorder = runRecorder;
        this.chapter10EvaluationService = chapter10EvaluationService;
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
        String runId = runRecorder.nextRunId();
        java.time.Instant startedAt = java.time.Instant.now();
        long startedNanos = System.nanoTime();
        List<EvalResult> results = evaluationRunner.run(cases);
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
        RunMetrics metrics = evaluationRunner.metrics(results, durationMillis);
        runRecorder.recordLegacyEvaluationRun(runId, "legacy-admin-evaluation", results, metrics, startedAt, java.time.Instant.now());
        return Map.of("runId", runId, "results", results, "metrics", metrics, "signals", List.of("cases:" + metrics.total(), "passed:" + metrics.passed(), "failed:" + metrics.failed(), "durationMs:" + metrics.durationMillis()));
    }

    @POST
    @Path("/runs")
    @Operation(
            summary = "Run chapter-10 evaluation cases",
            description = "Book chapter: 10. Lightweight case-based evaluation seam for manual, product, code, and multi-agent lanes, with inspection surfaced through the shared runtime run history."
    )
    @RequestBody(
            description = "Chapter-10 evaluation cases to execute.",
            required = true,
            content = @Content(schema = @Schema(implementation = Chapter10EvalRunRequest.class))
    )
    @APIResponse(
            responseCode = "200",
            description = "Chapter-10 evaluation run result.",
            content = @Content(schema = @Schema(implementation = Chapter10EvalRunResult.class))
    )
    public Chapter10EvalRunResult runChapter10(Chapter10EvalRunRequest request) {
        return chapter10EvaluationService.run(request);
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
