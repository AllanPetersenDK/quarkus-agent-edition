package dk.ashlan.agent.eval.gaia;

import java.util.List;

public record GaiaValidationResponse(
        String datasetUrl,
        int requestedLimit,
        int loadedCases,
        int selectedCases,
        GaiaValidationSummary summary,
        List<GaiaValidationCaseResult> results
) {
}
