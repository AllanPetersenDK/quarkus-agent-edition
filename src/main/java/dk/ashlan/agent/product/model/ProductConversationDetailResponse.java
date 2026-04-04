package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Detailed product conversation inspection result.")
public record ProductConversationDetailResponse(
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
        List<String> qualitySignals,
        @Schema(description = "All stored turns for the conversation.")
        List<ProductConversationTurn> turns
) {
    public ProductConversationDetailResponse {
        qualitySignals = qualitySignals == null ? List.of() : List.copyOf(qualitySignals);
        turns = turns == null ? List.of() : List.copyOf(turns);
    }

    public static ProductConversationDetailResponse from(ProductConversationState state) {
        return new ProductConversationDetailResponse(
                state.conversationId(),
                state.createdAt(),
                state.updatedAt(),
                state.turnCount(),
                state.lastRunId(),
                state.lastStatus(),
                state.lastQuery(),
                state.lastAnswer(),
                state.lastFailureReason(),
                state.lastQualitySignals(),
                state.turns()
        );
    }
}
