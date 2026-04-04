package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Single product conversation turn stored for operator inspection and persistence.")
public record ProductConversationTurn(
        @Schema(description = "Chapter-10 runtime run identifier for the turn.")
        String runId,
        @Schema(description = "Turn creation timestamp.")
        Instant createdAt,
        @Schema(description = "Original user query.")
        String query,
        @Schema(description = "Answer produced by the product pipeline.")
        String answer,
        @Schema(description = "Status such as COMPLETED or REJECTED.")
        String status,
        @Schema(description = "Number of retrieved sources used for this turn.")
        int sourceCount,
        @Schema(description = "Number of citations used for this turn.")
        int citationCount,
        @Schema(description = "Number of retrieval hits used for this turn.")
        int retrievalCount,
        @Schema(description = "Number of planning steps used for this turn.")
        int planStepCount,
        @Schema(description = "Quality signals captured for the turn.")
        List<String> qualitySignals,
        @Schema(description = "Failure reason when the pipeline rejected the answer, if any.")
        String failureReason
) {
    public ProductConversationTurn {
        qualitySignals = qualitySignals == null ? List.of() : List.copyOf(qualitySignals);
    }
}
