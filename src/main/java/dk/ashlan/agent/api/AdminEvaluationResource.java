package dk.ashlan.agent.api;

import dk.ashlan.agent.eval.EvalCase;
import dk.ashlan.agent.eval.EvalResult;
import dk.ashlan.agent.eval.EvaluationRunner;
import dk.ashlan.agent.eval.RunMetrics;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Path("/admin/evaluations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Internal Evaluation", description = "HTTP-exposed evaluation harness for chapter ten comparison and metrics runs.")
public class AdminEvaluationResource {
    private final EvaluationRunner evaluationRunner;

    public AdminEvaluationResource(EvaluationRunner evaluationRunner) {
        this.evaluationRunner = evaluationRunner;
    }

    @POST
    @Operation(
            summary = "Run evaluation cases",
            description = "Internal admin endpoint that runs chapter evaluation cases and returns both results and metrics. It exposes the evaluation harness over HTTP, but not the lower-level planning, memory, or orchestration internals."
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
}
