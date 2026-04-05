package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Compact product operator overview for closed-network operations and release-gate review.")
public record ProductOperatorOverviewResponse(
        @Schema(description = "Total number of persisted product conversations.")
        long conversationCount,
        @Schema(description = "Number of recent conversations included in this overview.")
        int recentConversationCount,
        @Schema(description = "Latest conversation identifier, when available.")
        String latestConversationId,
        @Schema(description = "Latest run identifier, when available.")
        String latestRunId,
        @Schema(description = "Latest conversation status, when available.")
        String latestStatus,
        @Schema(description = "Latest conversation update timestamp, when available.")
        Instant latestUpdatedAt,
        @Schema(description = "Latest conversation failure reason, when available.")
        String latestFailureReason,
        @Schema(description = "Recent product conversations, most recent first.")
        List<ProductConversationSummaryResponse> recentConversations,
        @Schema(description = "Small operator signals for closed-network drift checks.")
        List<String> signals
) {
    public ProductOperatorOverviewResponse {
        recentConversations = recentConversations == null ? List.of() : List.copyOf(recentConversations);
        signals = signals == null ? List.of() : List.copyOf(signals);
    }
}
