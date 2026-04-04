package dk.ashlan.agent.eval;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

public record Chapter10EvalRunRequest(
        @Schema(description = "Human-readable name for the evaluation run.")
        String name,
        @Schema(description = "Evaluation cases to execute.")
        List<Chapter10EvalCase> cases
) {
    public Chapter10EvalRunRequest {
        name = name == null ? "" : name.trim();
        cases = cases == null ? List.of() : List.copyOf(cases);
    }
}
