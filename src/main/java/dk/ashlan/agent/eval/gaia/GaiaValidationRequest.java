package dk.ashlan.agent.eval.gaia;

import jakarta.validation.constraints.Min;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record GaiaValidationRequest(
        @Min(1)
        @Schema(description = "Maximum number of filtered GAIA Level 1 cases to evaluate.", minimum = "1", defaultValue = "10")
        Integer limit
) {
    public int effectiveLimit(int defaultLimit) {
        return limit == null || limit < 1 ? defaultLimit : limit;
    }
}
