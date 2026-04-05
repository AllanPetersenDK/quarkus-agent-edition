package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Single product conversation turn stored for operator inspection and persistence.")
public record ProductConversationTurn(
        @Schema(description = "Chapter-10 runtime run identifier for the turn.")
        String runId,
        @Schema(description = "Turn start timestamp.")
        Instant startedAt,
        @Schema(description = "Turn completion timestamp.")
        Instant completedAt,
        @Schema(description = "Wall-clock duration in milliseconds.")
        long durationMs,
        @Schema(description = "Original user query.")
        String query,
        @Schema(description = "Answer produced by the product pipeline.")
        String answer,
        @Schema(description = "Status such as COMPLETED or REJECTED.")
        String status,
        @Schema(description = "Compact one-line summary of the turn.")
        String summary,
        @Schema(description = "Compact trace summary for the turn.")
        String traceSummary,
        @Schema(description = "Compact tool-usage summary for the turn.")
        String toolUsageSummary,
        @Schema(description = "Number of retrieved sources used for this turn.")
        int sourceCount,
        @Schema(description = "Number of citations used for this turn.")
        int citationCount,
        @Schema(description = "Number of retrieval hits used for this turn.")
        int retrievalCount,
        @Schema(description = "Number of planning steps used for this turn.")
        int planStepCount,
        @Schema(description = "Planning summary for the turn.")
        String planSummary,
        @Schema(description = "Reflection summary for the turn.")
        String reflectionSummary,
        @Schema(description = "Planning data for the turn.")
        ProductPlanResponse plan,
        @Schema(description = "Reflection data for the turn.")
        ProductReflectionResponse reflection,
        @Schema(description = "Knowledge sources used for the turn.")
        List<ProductSourceResponse> sources,
        @Schema(description = "Product artifacts associated with the turn.")
        List<ProductArtifactSummaryResponse> artifacts,
        @Schema(description = "Quality signals captured for the turn.")
        List<String> qualitySignals,
        @Schema(description = "Failure reason when the pipeline rejected the answer, if any.")
        String failureReason
) {
    public ProductConversationTurn {
        sources = sources == null ? List.of() : List.copyOf(sources);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
        qualitySignals = qualitySignals == null ? List.of() : List.copyOf(qualitySignals);
    }
}
