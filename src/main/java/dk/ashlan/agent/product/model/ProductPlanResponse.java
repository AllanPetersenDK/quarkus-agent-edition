package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ProductPlanResponse(
        @Schema(description = "Compact summary of the planning pass.")
        String summary,
        @Schema(description = "Most actionable next step identified by the planner.")
        String nextStep,
        @Schema(description = "Number of plan steps considered.")
        int stepCount
) {
}
