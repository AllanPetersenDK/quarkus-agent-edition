package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Small product health summary for the canonical product lane.")
public record ProductOverviewHealthSummaryResponse(
        @Schema(description = "Overall product health status.")
        String status,
        @Schema(description = "Short note suitable for a dashboard tile.")
        String note,
        @Schema(description = "Persisted conversation count.")
        long conversationCount,
        @Schema(description = "Product run count.")
        long runCount,
        @Schema(description = "Failed or rejected product run count.")
        long failureCount
) {
}
