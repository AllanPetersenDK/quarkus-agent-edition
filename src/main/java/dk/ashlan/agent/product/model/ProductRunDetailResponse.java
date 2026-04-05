package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Detailed product run inspection result.")
public record ProductRunDetailResponse(
        @Schema(description = "Stable run identifier.")
        String runId,
        @Schema(description = "Conversation identifier, when the run is conversation-scoped.")
        String conversationId,
        @Schema(description = "Original query or objective that triggered the run.")
        String query,
        @Schema(description = "Primary answer or output returned by the run.")
        String answer,
        @Schema(description = "Run input summary.")
        String inputSummary,
        @Schema(description = "Run output or answer summary.")
        String outputSummary,
        @Schema(description = "Compact one-line summary suitable for details panels.")
        String summary,
        @Schema(description = "Run status such as COMPLETED, REJECTED, or FAILED.")
        String status,
        @Schema(description = "Run start timestamp.")
        Instant startedAt,
        @Schema(description = "Run completion timestamp.")
        Instant completedAt,
        @Schema(description = "Wall-clock duration in milliseconds.")
        long durationMs,
        @Schema(description = "Compact trace summary.")
        String traceSummary,
        @Schema(description = "Compact tool-usage summary.")
        String toolUsageSummary,
        @Schema(description = "Compact planning summary.")
        String planSummary,
        @Schema(description = "Compact reflection summary.")
        String reflectionSummary,
        @Schema(description = "Plan data, if available.")
        ProductPlanResponse plan,
        @Schema(description = "Reflection data, if available.")
        ProductReflectionResponse reflection,
        @Schema(description = "Top knowledge sources used by the run.")
        List<ProductSourceResponse> sources,
        @Schema(description = "Product artifacts associated with the run.")
        List<ProductArtifactSummaryResponse> artifacts,
        @Schema(description = "Observed quality signals.")
        List<String> qualitySignals,
        @Schema(description = "Number of retrieved sources used for the run.")
        int sourceCount,
        @Schema(description = "Number of citations used for the run.")
        int citationCount,
        @Schema(description = "Number of retrieval hits used for the run.")
        int retrievalCount,
        @Schema(description = "Number of tool or tool-like steps used for the run.")
        int toolCount,
        @Schema(description = "Number of planning steps used for the run.")
        int planStepCount,
        @Schema(description = "Number of product artifacts associated with the run.")
        int artifactCount,
        @Schema(description = "Approval status when a reviewer or quality gate applies.")
        Boolean approved,
        @Schema(description = "Simple score when the lane computes one.")
        Double score,
        @Schema(description = "Reason a review or quality gate rejected the run, if any.")
        String rejectionReason,
        @Schema(description = "Reason a run failed, if any.")
        String failureReason,
        @Schema(description = "Simple error category for failures, if any.")
        String errorCategory
) {
    public ProductRunDetailResponse {
        qualitySignals = qualitySignals == null ? List.of() : List.copyOf(qualitySignals);
        sources = sources == null ? List.of() : List.copyOf(sources);
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
