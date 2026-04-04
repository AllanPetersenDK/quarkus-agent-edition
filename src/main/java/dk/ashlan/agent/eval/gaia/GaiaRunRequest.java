package dk.ashlan.agent.eval.gaia;

import jakarta.validation.constraints.Min;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record GaiaRunRequest(
        @Schema(description = "Optional Hugging Face dataset URL or local parquet/snapshot path.")
        String datasetUrl,
        @Schema(description = "Optional local snapshot path. Takes precedence over datasetUrl when set.")
        String localPath,
        @Schema(description = "Optional dataset config or top-level folder, for example 2023.")
        String config,
        @Schema(description = "Optional split, for example validation.")
        String split,
        @Schema(description = "Optional GAIA level filter. Defaults to 1 when omitted.")
        Integer level,
        @Min(1)
        @Schema(description = "Maximum number of cases to evaluate.", minimum = "1", defaultValue = "10")
        Integer limit,
        @Schema(description = "Stop the run after the first failing case.")
        Boolean failFast
) {
    public int effectiveLimit(int defaultLimit) {
        return limit == null || limit < 1 ? defaultLimit : limit;
    }

    public int effectiveLevel(int defaultLevel) {
        return level == null || level < 1 ? defaultLevel : level;
    }

    public boolean isFailFast() {
        return failFast != null && failFast;
    }
}
