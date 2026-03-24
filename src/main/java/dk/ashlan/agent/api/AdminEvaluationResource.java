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

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Path("/admin/evaluations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdminEvaluationResource {
    private final EvaluationRunner evaluationRunner;

    public AdminEvaluationResource(EvaluationRunner evaluationRunner) {
        this.evaluationRunner = evaluationRunner;
    }

    @POST
    public Map<String, Object> run(List<EvalCase> cases) {
        long startedAt = System.nanoTime();
        List<EvalResult> results = evaluationRunner.run(cases);
        long durationMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
        RunMetrics metrics = evaluationRunner.metrics(results, durationMillis);
        return Map.of("results", results, "metrics", metrics);
    }
}
