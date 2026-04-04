package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Compact product conversation summary for operator inspection.")
public record ProductConversationSummaryResponse(
        @Schema(description = "Stable conversation identifier.")
        String conversationId,
        @Schema(description = "Conversation creation timestamp.")
        Instant createdAt,
        @Schema(description = "Conversation last-update timestamp.")
        Instant updatedAt,
        @Schema(description = "Number of stored product turns.")
        int turnCount,
        @Schema(description = "Run identifier of the latest turn.")
        String lastRunId,
        @Schema(description = "Status of the latest turn.")
        String lastStatus,
        @Schema(description = "Latest query text.")
        String lastQuery,
        @Schema(description = "Latest answer text.")
        String lastAnswer,
        @Schema(description = "Latest failure reason, if any.")
        String lastFailureReason,
        @Schema(description = "Latest quality signals.")
        List<String> qualitySignals
) {
    public ProductConversationSummaryResponse {
        qualitySignals = qualitySignals == null ? List.of() : List.copyOf(qualitySignals);
    }

    public static ProductConversationSummaryResponse from(ProductConversationState state) {
        return new ProductConversationSummaryResponse(
                state.conversationId(),
                state.createdAt(),
                state.updatedAt(),
                state.turnCount(),
                state.lastRunId(),
                state.lastStatus(),
                state.lastQuery(),
                state.lastAnswer(),
                state.lastFailureReason(),
                state.lastQualitySignals()
        );
    }
}
