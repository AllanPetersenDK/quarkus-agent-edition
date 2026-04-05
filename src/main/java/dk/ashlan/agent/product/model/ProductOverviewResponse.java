package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Canonical product-lane overview for the assistant frontend.")
public record ProductOverviewResponse(
        @Schema(description = "Total number of persisted product conversations.")
        long totalConversations,
        @Schema(description = "Total number of recorded product runs.")
        long totalRuns,
        @Schema(description = "Number of active product runs.")
        long activeRuns,
        @Schema(description = "Number of completed product runs.")
        long completedRuns,
        @Schema(description = "Number of failed or rejected product runs.")
        long failedRuns,
        @Schema(description = "Number of recent conversations included in the overview.")
        int recentConversationCount,
        @Schema(description = "Number of recent runs included in the overview.")
        int recentRunCount,
        @Schema(description = "Number of recent failures included in the overview.")
        int recentFailureCount,
        @Schema(description = "Number of recent artifacts included in the overview.")
        int recentArtifactCount,
        @Schema(description = "Latest conversation identifier, when available.")
        String latestConversationId,
        @Schema(description = "Latest run identifier, when available.")
        String latestRunId,
        @Schema(description = "Latest conversation status, when available.")
        String latestStatus,
        @Schema(description = "Latest conversation update timestamp, when available.")
        Instant latestUpdatedAt,
        @Schema(description = "Small product health summary.")
        ProductOverviewHealthSummaryResponse health,
        @Schema(description = "Recent product conversations, most recent first.")
        List<ProductConversationSummaryResponse> recentConversations,
        @Schema(description = "Recent product runs, most recent first.")
        List<ProductRunSummaryResponse> recentRuns,
        @Schema(description = "Recent failed or rejected product runs.")
        List<ProductRunSummaryResponse> recentFailures,
        @Schema(description = "Recent product artifacts.")
        List<ProductArtifactSummaryResponse> recentArtifacts,
        @Schema(description = "Small product signals for dashboard use.")
        List<String> signals
) {
    public ProductOverviewResponse {
        recentConversations = recentConversations == null ? List.of() : List.copyOf(recentConversations);
        recentRuns = recentRuns == null ? List.of() : List.copyOf(recentRuns);
        recentFailures = recentFailures == null ? List.of() : List.copyOf(recentFailures);
        recentArtifacts = recentArtifacts == null ? List.of() : List.copyOf(recentArtifacts);
        signals = signals == null ? List.of() : List.copyOf(signals);
    }
}
