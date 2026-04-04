package dk.ashlan.agent.eval.gaia;

import java.util.List;
import java.util.Map;

public record GaiaRunResult(
        String runId,
        String datasetUrl,
        String localPath,
        String config,
        String split,
        Integer level,
        int limit,
        int total,
        int passed,
        int failed,
        long durationMillis,
        GaiaLevelSummary summary,
        Map<String, GaiaLevelSummary> summaryByLevel,
        List<GaiaCaseResult> sampleFailures,
        List<GaiaCaseResult> results
) {
}
