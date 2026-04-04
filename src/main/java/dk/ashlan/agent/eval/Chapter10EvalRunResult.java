package dk.ashlan.agent.eval;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record Chapter10EvalRunResult(
        @Schema(description = "Run identifier for the evaluation run.")
        String runId,
        @Schema(description = "Human-readable run name.")
        String name,
        @Schema(description = "Run creation timestamp.")
        Instant createdAt,
        @Schema(description = "Total number of cases executed.")
        int total,
        @Schema(description = "Number of cases that passed.")
        int passed,
        @Schema(description = "Number of cases that failed.")
        int failed,
        @Schema(description = "Average score across cases.")
        double averageScore,
        @Schema(description = "Short summary of the evaluation outcome.")
        String summary,
        @Schema(description = "Quality signals that describe the run.")
        List<String> qualitySignals,
        @Schema(description = "Per-case results.")
        List<Chapter10EvalCaseResult> results
) {
    public Chapter10EvalRunResult {
        qualitySignals = qualitySignals == null ? List.of() : List.copyOf(qualitySignals);
        results = results == null ? List.of() : List.copyOf(results);
    }
}
