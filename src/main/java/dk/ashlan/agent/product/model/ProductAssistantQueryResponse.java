package dk.ashlan.agent.product.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

public record ProductAssistantQueryResponse(
        @Schema(description = "Chapter-10 runtime run identifier for the product assistant query.")
        String runId,
        @Schema(description = "Conversation identifier used to scope session and memory continuity.")
        String conversationId,
        @Schema(description = "True when the query created a new persistent conversation record.")
        boolean conversationCreated,
        @Schema(description = "Number of messages currently stored for the conversation.")
        int conversationMessageCount,
        @Schema(description = "Number of stored product turns for the conversation after this query.")
        int conversationTurnCount,
        @Schema(description = "Original user query.")
        String query,
        @Schema(description = "Answer assembled from the product assistant pipeline.")
        String answer,
        @Schema(description = "Top knowledge sources used by the assistant.")
        List<ProductSourceResponse> sources,
        @Schema(description = "Relevant memory hints recovered for the conversation.")
        List<String> memoryHints,
        @Schema(description = "Compact planning summary used to shape the answer.")
        ProductPlanResponse plan,
        @Schema(description = "Reflection result showing whether the answer passed the lightweight quality check.")
        ProductReflectionResponse reflection,
        @Schema(description = "Small product-lane signals that make the run easy to inspect without exposing chapter internals.")
        List<String> signals,
        @Schema(description = "Conversation status such as COMPLETED or REJECTED.")
        String status,
        @Schema(description = "Failure reason when the product pipeline rejected the answer, if any.")
        String failureReason,
        @Schema(description = "Compact one-line summary for product list views.")
        String summary,
        @Schema(description = "Run start timestamp.")
        Instant startedAt,
        @Schema(description = "Run completion timestamp.")
        Instant completedAt,
        @Schema(description = "Wall-clock duration in milliseconds.")
        long durationMs,
        @Schema(description = "Compact trace summary for product dashboards.")
        String traceSummary,
        @Schema(description = "Compact tool-usage summary for product dashboards.")
        String toolUsageSummary,
        @Schema(description = "Number of product artifacts associated with the query.")
        int artifactCount,
        @Schema(description = "Product artifacts associated with the query.")
        List<ProductArtifactSummaryResponse> artifacts
) {
    public ProductAssistantQueryResponse {
        artifacts = artifacts == null ? List.of() : List.copyOf(artifacts);
    }
}
