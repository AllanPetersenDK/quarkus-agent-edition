package dk.ashlan.agent.api;

import dk.ashlan.agent.eval.AgentTrace;
import dk.ashlan.agent.eval.AgentTraceService;
import dk.ashlan.agent.eval.EvalCase;
import dk.ashlan.agent.eval.EvalResult;
import dk.ashlan.agent.eval.EvaluationRunner;
import dk.ashlan.agent.eval.RunMetrics;
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

    public AdminEvaluationResource(EvaluationRunner evaluationRunner, AgentTraceService traceService) {
        this.evaluationRunner = evaluationRunner;
        this.traceService = traceService;
    }

    @POST
    @Operation(
            summary = "Run evaluation cases",
            description = "Internal admin endpoint that runs chapter evaluation cases and returns both results and metrics. It exposes the evaluation harness over HTTP, but not the lower-level planning, memory, or orchestration internals."
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

    @GET
    @Path("/{caseId}")
    @Operation(
            summary = "Inspect an evaluation trace",
            description = "Read-only trace inspection seam for the most recent run of a named evaluation case. It exposes the stored execution trace without exposing the agent internals behind it."
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
