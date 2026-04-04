package dk.ashlan.agent.eval;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

public record Chapter10EvalCase(
        @Schema(description = "Case identifier.")
        String caseId,
        @Schema(description = "Target lane to evaluate, such as manual, product, code, or multi-agent.")
        String targetLane,
        @Schema(description = "Input prompt, question, or objective for the target lane.")
        String input,
        @Schema(description = "Expected substring to match in the output, if any.")
        String expectedSubstring,
        @Schema(description = "Expected signals that should appear in the run history or response metadata.")
        List<String> expectedSignals,
        @Schema(description = "Minimum score required to pass the case, if set.")
        Double minimumScore,
        @Schema(description = "Optional retrieval depth for product cases.")
        Integer topK
) {
    public Chapter10EvalCase {
        caseId = caseId == null ? "" : caseId.trim();
        targetLane = targetLane == null ? "" : targetLane.trim();
        input = input == null ? "" : input.trim();
        expectedSubstring = expectedSubstring == null ? "" : expectedSubstring.trim();
        expectedSignals = expectedSignals == null ? List.of() : List.copyOf(expectedSignals);
    }
}
